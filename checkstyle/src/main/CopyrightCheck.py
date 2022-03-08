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

import subprocess
import csv
import re
import datetime

#constants
import sys

COMMITTERS_CONFIG_FILE = ''
TEMPLATE_COPYRIGHT_FILE = ''
IGNORE_FILE = ''
if len(sys.argv) == 4:
    COMMITTERS_CONFIG_FILE = sys.argv[1]
    TEMPLATE_COPYRIGHT_FILE = sys.argv[2]
    IGNORE_FILE = sys.argv[3]

BANNER = '=' * 120

def main():
    print(BANNER + '\nCopyright Check Python Script:')
    PermissionsCheck()

    committerEmailExtension = GetCommitterEmailExtension()
    projectCommitters = ReadProjectCommittersConfigFile()

    CheckCommitterInConfigFile(committerEmailExtension, projectCommitters)

    alteredFiles = FindAlteredFiles()

    if alteredFiles:
        issueCounter = CheckCopyrightForFiles(alteredFiles, projectCommitters, committerEmailExtension)
    else:
        issueCounter = 0

    print(str(issueCounter) + ' issue(s) found after '+ str(len(alteredFiles)) + ' altered file(s) checked')
    print(BANNER)


# Check that Script has access to command line functions to use git
def PermissionsCheck():
   if 'permission denied' in subprocess.run('git', shell=True, stdout=subprocess.PIPE).stdout.decode('utf-8').lower():
       print('Error, I may not have the necessary permissions. Exiting...')
       print(BANNER)
       sys.exit()
   else:
       return

# Returns List of Strings of file tracked by git which have been changed/added
def FindAlteredFiles():
    ignoreFilePaths = GetIgnoredFiles()

    #Before Stage lower case d removes deleted files
    stream = subprocess.run('git diff --name-only --diff-filter=d', shell=True, stdout=subprocess.PIPE)
    fileNames = stream.stdout.decode('utf-8')
    #Staged
    stream = subprocess.run('git diff --name-only --cached --diff-filter=d', shell=True, stdout=subprocess.PIPE)
    fileNames += '\n' + stream.stdout.decode('utf-8')
    #New committed
    stream = subprocess.run('git diff --name-only HEAD^ HEAD --diff-filter=d', shell=True, stdout=subprocess.PIPE)
    fileNames += '\n' + stream.stdout.decode('utf-8')

    #String to list of strings
    alteredFiles = fileNames.split("\n")

    #Remove duplicates
    alteredFiles = list(dict.fromkeys(alteredFiles))

    #Remove blank string(s)
    alteredFiles = list(filter(None, alteredFiles))

    #Remove ignored-extensions
    alteredFiles = list(filter(lambda fileName: not re.match("|".join(ignoreFilePaths), fileName), alteredFiles))

    return alteredFiles

# Get the email of the most recent committer
def GetCommitterEmailExtension():
    email = subprocess.run('git show -s --format=\'%ce\'', shell=True, stdout=subprocess.PIPE).stdout.decode('utf-8').rstrip('\n')
    return email[email.index('@'):]

# Read the config file with names of companies and respective email extensions
def ReadProjectCommittersConfigFile():
    try:
        with open(COMMITTERS_CONFIG_FILE, 'r') as file:
            reader = csv.reader(file, delimiter=',')
            projectCommitters = {row[0]:row[1] for row in reader}
        projectCommitters.pop('email')  #Remove csv header
    except FileNotFoundError:
        print('Unable to open Project Committers Config File, have the command line arguments been set?')
        print(BANNER)
        sys.exit()
    return projectCommitters

def CheckCommitterInConfigFile(committerEmailExtension, projectCommitters):
    if not committerEmailExtension in projectCommitters:
        print('Error, Committer email is not included in config file.')
        print('If your company is new to the project please make appropriate changes to project-committers-config.csv')
        print('for Copyright Check to work.')
        print('Exiting...')
        print(BANNER)
        sys.exit()
    else:
        return True

# Read config file with list of files to ignore
def GetIgnoredFiles():
    try:
        with open(IGNORE_FILE, 'r') as file:
            reader = csv.reader(file)
            ignoreFilePaths = [row[0] for row in reader]
        ignoreFilePaths.pop(0)  #Remove csv header
        ignoreFilePaths = [filePath.replace('*', '.*') for filePath in ignoreFilePaths]
    except FileNotFoundError:
        print('Unable to open File Ignore Config File, have the command line arguments been set?')
        print(BANNER)
        sys.exit()
    return ignoreFilePaths

# Read the template copyright file
def GetCopyrightTemplate():
    try:
        with open(TEMPLATE_COPYRIGHT_FILE, 'r') as file:
            copyrightTemplate = file.readlines()
    except FileNotFoundError:
        print('Unable to open Template Copyright File, have the command line arguments been set?')
        print(BANNER)
        sys.exit()
    return copyrightTemplate

def GetProjectRootDir():
    return subprocess.run('git rev-parse --show-toplevel', shell=True, stdout=subprocess.PIPE).stdout.decode('utf-8').rstrip('\n') + '/'

# Get the Copyright from the altered file
def ParseFileCopyright(fileObject):
    global issueCounter
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
    # Comment Characters can very depending on file # *..
    endOfCommentsIndex = list(fileCopyright.values())[0].index('=')
    for key in fileCopyright:
        fileCopyright[key] = fileCopyright[key][endOfCommentsIndex:]
        if fileCopyright[key] == '':
            fileCopyright[key] = '\n'

    return fileCopyright

def CheckCopyrightForFiles(alteredFiles, projectCommitters, committerEmailExtension):
    issueCounter = 0
    templateCopyright = GetCopyrightTemplate() #Get Copyright Template
    projectRootDir = GetProjectRootDir()

    for fileName in alteredFiles: # Not removed files
        try:
            with open(projectRootDir + fileName, 'r') as fileObject:
                (fileCopyright, fileSignatures) = ParseFileCopyright(fileObject)

            #Empty dict evaluates to bool false
            if fileCopyright and fileSignatures:
                fileCopyright = RemoveCommentBlock(fileCopyright)
                issueCounter += CheckCopyrightFormat(fileCopyright, templateCopyright, projectRootDir + fileName)
                committerCompany = projectCommitters[committerEmailExtension]
                issueCounter += CheckCopyrightSignature(fileSignatures, committerCompany, projectRootDir + fileName)
            else:
                issueCounter += 1

        except FileNotFoundError:
            issueCounter += 1
            print('Unable to find file ' + projectRootDir + fileName)
    return issueCounter

# Check that the filecopyright matches the template copyright and print comparison
def CheckCopyrightFormat(copyrightInFile, templateCopyright, filePath):
    issueCounter = 0
    errorWithComparison = ''
    for copyrightInFileKey, templateLine in zip(copyrightInFile, templateCopyright):
        if copyrightInFile[copyrightInFileKey] != templateLine:
            issueCounter += 1
            errorWithComparison += filePath + ' | line ' + '{:2}'.format(copyrightInFileKey) + ' read \t  ' + repr(copyrightInFile[copyrightInFileKey]) + '\n'
            errorWithComparison += filePath + ' | line ' + '{:2}'.format(copyrightInFileKey) + ' expected ' + repr(templateLine) + '\n'
    if errorWithComparison != '':
        print(errorWithComparison.rstrip('\n'))
    return issueCounter

# Check the signatures and compare with committer signature and current year
def CheckCopyrightSignature(copyrightSignatures, committerCompany, filePath):
    issueCounter = 0
    errorWithSignature = ''
    signatureExists = False #signatureExistsForCommitter
    afterFirstLine = False #afterFirstCopyright
    for key in copyrightSignatures:
        if afterFirstLine and 'Modifications Copyright' not in copyrightSignatures[key]:
            issueCounter += 1
            errorWithSignature += filePath + ' | line ' + str(key) + ' expected Modifications Copyright\n'
        elif not afterFirstLine and 'Copyright' not in copyrightSignatures[key]:
            issueCounter += 1
            errorWithSignature += filePath + ' | line ' + str(key) + ' expected Copyright\n'
        if committerCompany in copyrightSignatures[key]:
            signatureExists = True
            signatureYear = int(re.findall(r'\d+', copyrightSignatures[key])[-1])
            currentYear = datetime.date.today().year
            if signatureYear != currentYear:
                issueCounter += 1
                errorWithSignature += filePath + ' | line ' + str(key) + ' update year to include ' + str(currentYear) + '\n'
        afterFirstLine = True

    if not signatureExists:
        issueCounter += 1
        errorWithSignature += filePath + ' | missing company name and year for ' + committerCompany

    if errorWithSignature != '':
        print(errorWithSignature.rstrip('\n'))

    return issueCounter

if __name__ == '__main__':
    main()