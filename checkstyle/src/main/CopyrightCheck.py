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
CONFIGFILE = 'resources/copyright-check-config.csv'
EXAMPLECOPYRIGHTFILE = 'resources/copyright-example.txt'
BANNER = '=' * 120

def main():
    print(BANNER + '\n' "Copyright Check Python Script:")

    if not PermissionsCheck():
        print('Error, I may not have the necessary permissions. Exiting...')
        print(BANNER)
        return

    committerDetails = GetCommitterDetails()
    projectCommitters = ReadProjectCommittersConfigFile()

    if not committerDetails.get('email').endswith(tuple(projectCommitters)):
        print('Error, Committer email is not included in config file.')
        print('If your company is new to the project please make appropriate changes to copyright-check-config.csv')
        print('for Copyright Check to work.')
        print('Exiting...')
        print(BANNER)
        return

    alteredFiles = FindAlteredFiles()
    print(alteredFiles)

    if len(alteredFiles) > 0:
        print(str(len(alteredFiles)) + " File(s) have been changed")
    else:
        print("No file changes detected by git. Exiting...")
        print(BANNER)
        return

    exampleCopyright = GetCopyrightExample()

    projectRootDir = os.popen('git rev-parse --show-toplevel').read().replace('\n','/')

    print(BANNER)
    for fileName in alteredFiles:
        noCopyrightError = True
        try:
            with open(projectRootDir + fileName, 'r') as fileObject:
                (fileCopyright, fileSignatures) = ParseFileCopyright(fileObject)

            if not (len(fileCopyright) == 0 and len(fileSignatures) == 0):
                fileCopyright = RemoveCommentBlock(fileCopyright, exampleCopyright[0][0])
                noCopyrightError &= CheckCopyrightFormat(fileCopyright, exampleCopyright, fileName)
                committerEmailExtension = committerDetails['email'][committerDetails['email'].index('@'):]
                committerCompany = projectCommitters[committerEmailExtension]
                noCopyrightError &= CheckCopyrightSignature(fileSignatures, committerCompany, fileName, committerDetails['email'])
                if noCopyrightError:
                    print('No copyright errors found for file ' + fileName)

        except FileNotFoundError:
            print('Unable to find file ' + fileName + '. If file has been intentionally deleted this is ok.')
        print(BANNER)



# Check that Script has access to command line functions to use git
def PermissionsCheck():
    try:
        os.popen('git')
    except:
        return False
    return True

# Returns List of Strings of file tracked by git which have been changed/added
def FindAlteredFiles():
    #Before Stage
    stream = os.popen('git diff --name-only').read()
    #Staged
    stream += os.popen('git diff --name-only --cached').read()
    #New committed
    stream += os.popen('git diff --name-only HEAD^ HEAD').read()

    #String to list of strings
    alteredFiles = stream.split("\n")

    #Remove duplicates
    alteredFiles = list(dict.fromkeys(alteredFiles))

    #Remove blank string(s)
    alteredFiles = list(filter(None, alteredFiles))

    #Remove copyright-example
    alteredFiles.remove(next(file for file in alteredFiles if 'copyright-example.txt' in file))

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
    with open(CONFIGFILE, 'r') as file:
        reader = csv.reader(file)
        projectCommitters = {row[0]:row[1] for row in reader}
    projectCommitters.pop('email')  #Remove csv header
    return projectCommitters

# Read the example copyright file
def GetCopyrightExample():
    with open(EXAMPLECOPYRIGHTFILE, 'r') as file:
        copyrightExample = file.readlines()
    return copyrightExample

# Get the Copyright from the altered file
def ParseFileCopyright(fileObject):
    copyrightFlag = False
    copyrightInFile = []
    for line in fileObject:
        if 'LICENSE_START' in line:
            copyrightFlag = True
        if copyrightFlag:
            copyrightInFile.append(line)
        if 'LICENSE_END' in line:
            break

    if not copyrightFlag:
        print('No copyright found for ' + fileObject.name)
        return ([],[])

    copyrightSignatures = []
    #Capture signature lines after LICENSE_START line
    for line in copyrightInFile[1:]:
        if '=' not in line:
            copyrightSignatures.append(line)
            copyrightInFile.remove(line)
        else:
            break

    return (copyrightInFile, copyrightSignatures)

# Remove the Block comment syntax
def RemoveCommentBlock(fileCopyright, firstExpectedCharacter):
    # Comment Characters can very depending on file # *...
    commentCharacters = fileCopyright[0][:fileCopyright[0].index(firstExpectedCharacter)]
    i = 0
    while i < len(fileCopyright):
        # Replace only first instance of comment characters including spaces,
        # where this fails replace comment character without spaces
        fileCopyright[i] = fileCopyright[i].replace(commentCharacters, '', 1).replace(commentCharacters.replace(' ',''),'')
        i+=1
    return fileCopyright

# Check that the filecopyright matches the example copyright and print comparison
def CheckCopyrightFormat(copyrightInFile, exampleCopyright, fileName):
    errorWithComparison = ''
    lineNumber = 0
    for inFileLine, exampleLine in zip(copyrightInFile, exampleCopyright):
        if inFileLine != exampleLine and not BlankLineComparison(inFileLine, exampleLine):
            errorWithComparison += 'Read Line ' + '{:2}'.format(lineNumber) + '\t ' + repr(inFileLine) + '\nExpected Line ' + '{:2}'.format(lineNumber) + ' ' + repr(exampleLine) + '\n'
        lineNumber += 1
    if errorWithComparison != '':
        errorWithComparison = 'COPYRIGHT FORMAT WARNING IN ' + fileName + '\n' + errorWithComparison + '\n'
        print(errorWithComparison)
    return errorWithComparison == ''

# Check whitespace comparisons
def BlankLineComparison(inFileLine, exampleLine):
    return inFileLine.replace(' ','') == '\n' and exampleLine.replace(' ','') == '\n'

# Check the signatures and compare with committer signature and current year
def CheckCopyrightSignature(copyrightSignatures, committerCompany, fileName, committerEmail):
    errorWithSignature = ''
    signatureExists = False
    afterFirstLine = False
    for line in copyrightSignatures:
        if afterFirstLine and 'Modifications Copyright' not in line:
            errorWithSignature += line.replace('\n','') + ' | expected Modifications Copyright on line\n'
        elif not afterFirstLine and 'Copyright' not in line:
            errorWithSignature += line.replace('\n','') + ' | expected Copyright on line\n'
        if committerCompany in line:
            signatureExists = True
            signatureYear = int(re.findall(r'\d+', line)[-1])
            currentYear = date.today().year
            if signatureYear != currentYear:
                errorWithSignature += line.replace('\n','') + ' | update year to ' + str(currentYear) + '\n'
        afterFirstLine = True

    if not signatureExists:
        errorWithSignature = ''.join(copyrightSignatures) + 'Missing company name and year for committer ' + committerEmail + ' at ' + committerCompany

    if errorWithSignature != '':
        errorWithSignature = 'COPYRIGHT SIGNATURE WARNING IN ' + fileName + '\n' + errorWithSignature
        print(errorWithSignature.rstrip('\n'))

    return errorWithSignature == ''

main()