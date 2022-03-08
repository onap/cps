#  ============LICENSE_START=======================================================
#  Copyright (C) 2022 Nordix Foundation
#  ================================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  ============LICENSE_END=========================================================

import os
import csv
import re
from datetime import date

#constants
CONFIG_FILE = 'resources/project-committers-config.csv'
TEMPLATE_COPYRIGHT_FILE = 'resources/copyright-template.txt'
IGNORE_FILE = 'resources/ignore-files-config.csv'
BANNER = '=' * 120

#global variables
issues = 0

def main():
    global issues
    print(BANNER + '\n' "Copyright Check Python Script:")

    if not PermissionsCheck():
        print('Error, I may not have the necessary permissions. Exiting...')
        print(BANNER)
        return

    committerDetails = GetCommitterDetails()
    projectCommitters = ReadProjectCommittersConfigFile()

    if not committerDetails.get('email').endswith(tuple(projectCommitters)):
        print('Error, Committer email is not included in config file.')
        print('If your company is new to the project please make appropriate changes to project-committers-config.csv')
        print('for Copyright Check to work.')
        print('Exiting...')
        print(BANNER)
        return

    ignoreFilePaths = GetIgnoredFiles()
    alteredFiles = FindAlteredFiles(ignoreFilePaths)

    if not len(alteredFiles) > 0:
        print("No file changes detected by git.")
        print(BANNER)
        return

    templateCopyright = GetCopyrightTemplate() #Get Copyright Template

    projectRootDir = os.popen('git rev-parse --show-toplevel').read().replace('\n','/')

    for fileName in alteredFiles: # Not removed files
        noCopyrightError = True
        try:
            with open(projectRootDir + fileName, 'r') as fileObject:
                (fileCopyright, fileSignatures) = ParseFileCopyright(fileObject)

            #Empty dict evaluates to bool false
            if fileCopyright and fileSignatures:
                fileCopyright = RemoveCommentBlock(fileCopyright)
                noCopyrightError &= CheckCopyrightFormat(fileCopyright, templateCopyright, projectRootDir + fileName)
                committerEmailExtension = committerDetails['email'][committerDetails['email'].index('@'):]
                committerCompany = projectCommitters[committerEmailExtension]
                noCopyrightError &= CheckCopyrightSignature(fileSignatures, committerCompany, projectRootDir + fileName)

        except FileNotFoundError:
            issues += 1
            print('Unable to find file ' + fileName)
    print(str(issues) + ' issue(s) found after '+ str(len(alteredFiles)) + ' file(s) checked')
    print(BANNER)


# Check that Script has access to command line functions to use git
def PermissionsCheck():
    try:
        os.popen('git')
    except:
        return False
    return True

# Returns List of Strings of file tracked by git which have been changed/added
def FindAlteredFiles(ignoreFilePaths):
    #Before Stage lower case d removes deleted files
    stream = os.popen('git diff --name-only --diff-filter=d').read()
    #Staged
    stream += os.popen('git diff --name-only --cached --diff-filter=d').read()
    #New committed
    stream += os.popen('git diff --name-only HEAD^ HEAD --diff-filter=d').read()

    #String to list of strings
    alteredFiles = stream.split("\n")

    #Remove duplicates
    alteredFiles = list(dict.fromkeys(alteredFiles))

    #Remove blank string(s)
    alteredFiles = list(filter(None, alteredFiles))

    #Remove ignored-extensions
    alteredFiles = list(filter(lambda fileName: not re.match("|".join(ignoreFilePaths), fileName), alteredFiles))


    return alteredFiles

# Get the details of the most recent committer as a dictionary
def GetCommitterDetails():
    stream = os.popen('git show -s --format=\'%h %cn %ce\'').read()
    authorDetailsList = stream.split()
    return {
        'hash': authorDetailsList[0],
        'username': authorDetailsList[1],
        'email': authorDetailsList[2]
    }

# Read the config file with names of companies and respective email extensions
def ReadProjectCommittersConfigFile():
    with open(CONFIG_FILE, 'r') as file:
        reader = csv.reader(file)
        projectCommitters = {row[0]:row[1] for row in reader}
    projectCommitters.pop('email')  #Remove csv header
    return projectCommitters

# Read config file with list of files to ignore
def GetIgnoredFiles():
    with open(IGNORE_FILE, 'r') as file:
        reader = csv.reader(file)
        ignoreFilePaths = [row[0] for row in reader]
    ignoreFilePaths.pop(0)  #Remove csv header
    ignoreFilePaths = [filePath.replace('*', '.*') for filePath in ignoreFilePaths]
    return ignoreFilePaths

# Read the template copyright file
def GetCopyrightTemplate():
    with open(TEMPLATE_COPYRIGHT_FILE, 'r') as file:
        copyrightTemplate = file.readlines()
    return copyrightTemplate

# Get the Copyright from the altered file
def ParseFileCopyright(fileObject):
    global issues
    copyrightFlag = False
    copyrightInFile = {}
    lineNumber = 1
    for line in fileObject:
        if 'LICENSE_START' in line:
            copyrightFlag = True
        if copyrightFlag:
            copyrightInFile[lineNumber] = line
        if 'LICENSE_END' in line:
            break
        lineNumber += 1

    if not copyrightFlag:
        print(fileObject.name + ' | no copyright found')
        issues += 1
        return {}, {}

    copyrightSignatures = {}
    copyrightLineNumbers = list(copyrightInFile.keys())
    #Capture signature lines after LICENSE_START line
    for lineNumber in copyrightLineNumbers:
        if '=' not in copyrightInFile[lineNumber]:
            copyrightSignatures[lineNumber] = copyrightInFile[lineNumber]
            copyrightInFile.pop(lineNumber)
        elif 'LICENSE_START' not in copyrightInFile[lineNumber]:
            break

    return (copyrightInFile, copyrightSignatures)

# Remove the Block comment syntax
def RemoveCommentBlock(fileCopyright):
    # Comment Characters can very depending on file # *... COULD just be index
    endOfCommentsIndex = list(fileCopyright.values())[0].index('=')
    for key in fileCopyright:
        fileCopyright[key] = fileCopyright[key][endOfCommentsIndex:]
        if fileCopyright[key] == '':
            fileCopyright[key] = '\n'

    return fileCopyright

# Check that the filecopyright matches the template copyright and print comparison
def CheckCopyrightFormat(copyrightInFile, templateCopyright, filePath):
    global issues
    errorWithComparison = ''
    for copyrightInFileKey, templateLine in zip(copyrightInFile, templateCopyright):
        if copyrightInFile[copyrightInFileKey] != templateLine:
            issues += 1
            errorWithComparison += filePath + ' | line ' + '{:2}'.format(copyrightInFileKey) + ' read \t   ' + repr(copyrightInFile[copyrightInFileKey]) + '\n'
            errorWithComparison += filePath + ' | line ' + '{:2}'.format(copyrightInFileKey) + ' expected ' + repr(templateLine) + '\n'
    if errorWithComparison != '':
        print(errorWithComparison.rstrip('\n'))
    return errorWithComparison == ''

# Check the signatures and compare with committer signature and current year
def CheckCopyrightSignature(copyrightSignatures, committerCompany, filePath):
    global issues
    errorWithSignature = ''
    signatureExists = False #signatureExistsForCommitter
    afterFirstLine = False #afterFirstCopyright
    for key in copyrightSignatures:
        if afterFirstLine and 'Modifications Copyright' not in copyrightSignatures[key]:
            issues += 1
            errorWithSignature += filePath + ' | line ' + str(key) + ' expected Modifications Copyright\n'
        elif not afterFirstLine and 'Copyright' not in copyrightSignatures[key]:
            issues += 1
            errorWithSignature += filePath + ' | line ' + str(key) + ' expected Copyright on line\n'
        if committerCompany in copyrightSignatures[key]:
            signatureExists = True
            signatureYear = int(re.findall(r'\d+', copyrightSignatures[key])[-1])
            currentYear = date.today().year
            if signatureYear != currentYear:
                issues += 1
                errorWithSignature += filePath + ' | line ' + str(key) + ' update year to include ' + str(currentYear) + '\n'
        afterFirstLine = True

    if not signatureExists:
        issues += 1
        errorWithSignature = filePath + ' | missing company name and year for ' + committerCompany

    if errorWithSignature != '':
        print(errorWithSignature.rstrip('\n'))

    return errorWithSignature == ''

main()