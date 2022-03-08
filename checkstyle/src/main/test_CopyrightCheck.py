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

import datetime
import sys
import unittest
from unittest import mock
from unittest.mock import MagicMock
import io

import CopyrightCheck

BANNER = '=' * 120

def MockStdout(command):
    mock_stdout = MagicMock()
    mock_stdout.configure_mock(**{"stdout.decode.return_value": command})
    return mock_stdout

class TestCopyrightCheck(unittest.TestCase):

    @mock.patch('subprocess.run')
    def test_PermissionsCheckFalse(self, mock_subprocess_run):
        mock_subprocess_run.return_value = MockStdout('Permission denied')

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        with self.assertRaises(SystemExit):
            CopyrightCheck.PermissionsCheck()
        sys.stdout = sys.__stdout__

        self.assertEqual(capturedOutput.getvalue(),
                         'Error, I may not have the necessary permissions. Exiting...\n' + BANNER + '\n')

    @mock.patch('subprocess.run')
    def test_PermissionsCheckTrue(self, mock_subprocess_run):
        mock_subprocess_run.return_value = MockStdout(
            'usage: git [--version] [--help] [-C <path>] [-c <name>=<value>]...')
        CopyrightCheck.PermissionsCheck()   # Assert no error thrown

    @mock.patch('CopyrightCheck.GetIgnoredFiles')
    @mock.patch('subprocess.run')
    def test_FindAlteredFiles(self, mock_subprocess_run, mock_GetIgnoredFiles):
        mock_GetIgnoredFiles.return_value = ['.*.json', 'dir/.*']
        mock_subprocess_run.return_value = MockStdout('File1.json\nFile2.java\nFile2.java\ndir/File3.java')
        result = CopyrightCheck.FindAlteredFiles()
        # Duplicates, .json and files in 'dir' removed
        self.assertEqual(result, ['File2.java'])

    @mock.patch('CopyrightCheck.GetIgnoredFiles')
    @mock.patch('subprocess.run')
    def test_FindAlteredFilesWithNoFileChanges(self, mock_subprocess_run, mock_GetIgnoredFiles):
        mock_GetIgnoredFiles.return_value = ['.*.json', 'dir/.*']
        mock_subprocess_run.return_value = MockStdout('File1.json\ndir/File3.java')
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        result = CopyrightCheck.FindAlteredFiles()
        sys.stdout = sys.__stdout__

        self.assertEqual(result, [])
        self.assertEqual(capturedOutput.getvalue(), '')

    @mock.patch('subprocess.run')
    def test_GetCommitterEmailExtension(self, mock_subprocess_run):
        mock_subprocess_run.return_value = MockStdout('a.committer.name@address.com')
        result = CopyrightCheck.GetCommitterEmailExtension()
        self.assertEqual(result, '@address.com')

    def test_ReadProjectCommittersConfigFile(self):
        mock_open = mock.mock_open(read_data="email,signature\n@address.com,Company Name")
        with mock.patch('builtins.open', mock_open):
            result = CopyrightCheck.ReadProjectCommittersConfigFile()
        self.assertEqual(result, {'@address.com': 'Company Name'})

    @mock.patch('CopyrightCheck.open')
    def test_ReadProjectCommittersConfigFileError(self, mock_OpenFile):
        mock_OpenFile.side_effect = FileNotFoundError
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        with self.assertRaises(SystemExit):
            CopyrightCheck.ReadProjectCommittersConfigFile()
        sys.stdout = sys.__stdout__
        expectedOutput = ('Unable to open Project Committers Config File, have the command line arguments been set?\n' +
                          BANNER + '\n')
        self.assertEqual(capturedOutput.getvalue(), expectedOutput)

    def test_CheckCommitterInConfigFileTrue(self):
        committerEmailExtension = '@address.com'
        projectCommitters = {'@address.com': 'Company Name'}
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        result = CopyrightCheck.CheckCommitterInConfigFile(committerEmailExtension, projectCommitters)
        sys.stdout = sys.__stdout__
        self.assertTrue(result)
        self.assertEqual(capturedOutput.getvalue(), "")

    def test_CheckCommitterInConfigFileFalse(self):
        committerEmailExtension = '@address.com'
        projectCommitters = {'@anotheraddress.com': 'Another Company Name'}
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        with self.assertRaises(SystemExit):
            CopyrightCheck.CheckCommitterInConfigFile(committerEmailExtension, projectCommitters)
        sys.stdout = sys.__stdout__
        expectedOutput = ('Error, Committer email is not included in config file.\n' +
                          'If your company is new to the project please make appropriate changes to project-committers-config.csv\n' +
                          'for Copyright Check to work.\n' +
                          'Exiting...\n' + BANNER + '\n')
        self.assertEqual(capturedOutput.getvalue(), expectedOutput)

    def test_GetIgnoredFiles(self):
        mock_open = mock.mock_open(read_data="file path\n*checkstyle/*\n*.json")
        with mock.patch('builtins.open', mock_open):
            result = CopyrightCheck.GetIgnoredFiles()
        self.assertEqual(result, [".*checkstyle/.*", ".*.json"])

    @mock.patch('CopyrightCheck.open')
    def test_GetIgnoredFilesError(self, mock_OpenFile):
        mock_OpenFile.side_effect = FileNotFoundError
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        with self.assertRaises(SystemExit):
            CopyrightCheck.GetIgnoredFiles()
        sys.stdout = sys.__stdout__
        expectedOutput = ('Unable to open File Ignore Config File, have the command line arguments been set?\n' +
                          BANNER + '\n')
        self.assertEqual(capturedOutput.getvalue(), expectedOutput)

    def test_GetCopyrightTemplate(self):
        mock_open = mock.mock_open(read_data="****\nThis is a\nCopyright File\n****")
        with mock.patch('builtins.open', mock_open):
            result = CopyrightCheck.GetCopyrightTemplate()
        self.assertEqual(result, ["****\n", "This is a\n", "Copyright File\n", "****"])

    @mock.patch('CopyrightCheck.open')
    def test_GetCopyrightTemplateError(self, mock_OpenFile):
        mock_OpenFile.side_effect = FileNotFoundError
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        with self.assertRaises(SystemExit):
            CopyrightCheck.GetCopyrightTemplate()
        sys.stdout = sys.__stdout__
        expectedOutput = ('Unable to open Template Copyright File, have the command line arguments been set?\n' +
                          BANNER + '\n')
        self.assertEqual(capturedOutput.getvalue(), expectedOutput)

    @mock.patch('subprocess.run')
    def test_GetProjectRootDir(self, mock_subprocess_run):
        mock_subprocess_run.return_value = MockStdout('project/root/dir\n')
        result = CopyrightCheck.GetProjectRootDir()
        self.assertEqual(result, 'project/root/dir/')


    def test_ParseFileCopyright(self):
        readFromFile = ["#Before lines will not be included\n",
                        "#===LICENSE_START===\n",
                        "#Copyright (C) 0000 Some Company\n",
                        "#A line without signature\n",
                        "#===============================\n",
                        "#This is the start of the Copyright\n",
                        "#===LICENSE_END===\n",
                        "After lines will not be included"]
        copyright, signatures = CopyrightCheck.ParseFileCopyright(readFromFile)
        self.assertEqual(copyright, {2: "#===LICENSE_START===\n",
                                     5: "#===============================\n",
                                     6: "#This is the start of the Copyright\n",
                                     7: "#===LICENSE_END===\n"})
        self.assertEqual(signatures, {3: "#Copyright (C) 0000 Some Company\n",
                                      4: "#A line without signature\n"})

    def test_ParseFileCopyrightNoCopyright(self):
        fileObject = io.StringIO("#This is not\na copyright\n")
        fileObject.name = 'some/file/name'

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        copyright, signatures = CopyrightCheck.ParseFileCopyright(fileObject)
        sys.stdout = sys.__stdout__

        self.assertEqual(copyright, {})
        self.assertEqual(signatures, {})
        self.assertEqual(capturedOutput.getvalue(), 'some/file/name | no copyright found\n')

    def test_RemoveCommentBlock(self):
        commentCharactersList = ['# ', '* ', '#  ', '*  ']

        for commentCharacters in commentCharactersList:
            copyright = {1: commentCharacters + '===LICENSE_START===\n',
                         2: '\n',
                         3: commentCharacters + 'This is the License\n',
                         4: commentCharacters + '===LICENSE_END===\n'}
            result = CopyrightCheck.RemoveCommentBlock(copyright)
            self.assertEqual(result, {1: '===LICENSE_START===\n',
                                      2: '\n',
                                      3: 'This is the License\n',
                                      4: '===LICENSE_END===\n'})

    @mock.patch('CopyrightCheck.open')
    @mock.patch('CopyrightCheck.GetProjectRootDir')
    @mock.patch('CopyrightCheck.GetCopyrightTemplate')
    def test_CheckCopyrightForFileNotFound(self, mock_GetCopyrightTemplate, mock_GetProjectRootDir, mock_OpenFile):
        mock_GetCopyrightTemplate.return_value = 'some-copyright-template'
        mock_GetProjectRootDir.return_value = 'some/project/root/dir/'
        mock_OpenFile.side_effect = FileNotFoundError

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        result = CopyrightCheck.CheckCopyrightForFiles(['some-file.java'], {}, [])
        sys.stdout = sys.__stdout__

        self.assertEqual(capturedOutput.getvalue(), 'Unable to find file some/project/root/dir/some-file.java\n')
        self.assertEqual(result, 1)

    @mock.patch('CopyrightCheck.ParseFileCopyright')
    @mock.patch('CopyrightCheck.GetProjectRootDir')
    @mock.patch('CopyrightCheck.GetCopyrightTemplate')
    def test_CheckCopyrightForFileWithNoCopyright(self, mock_GetCopyrightTemplate, mock_GetProjectRootDir,
                                                  mock_ParseFileCopyright):
        mock_GetCopyrightTemplate.return_value = 'some-copyright-template'
        mock_GetProjectRootDir.return_value = 'some/project/root/dir/'
        mock_ParseFileCopyright.return_value = ({}, {})
        mock_open = mock.mock_open(read_data="some-file-content")

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        with mock.patch('builtins.open', mock_open):
            result = CopyrightCheck.CheckCopyrightForFiles(['some-file.java'], {}, [])
        sys.stdout = sys.__stdout__

        self.assertEqual(capturedOutput.getvalue(), "")
        self.assertEqual(result, 1)


    @mock.patch('CopyrightCheck.CheckCopyrightSignature')
    @mock.patch('CopyrightCheck.CheckCopyrightFormat')
    @mock.patch('CopyrightCheck.ParseFileCopyright')
    @mock.patch('CopyrightCheck.GetProjectRootDir')
    @mock.patch('CopyrightCheck.GetCopyrightTemplate')
    def test_CheckCopyrightForFilesWhichAreRight(self, mock_GetCopyrightTemplate, mock_GetProjectRootDir,
                                                  mock_ParseFileCopyright, mock_CheckCopyrightFormat,
                                                  mock_CheckCopyrightSignature):
        mock_GetCopyrightTemplate.return_value = 'some-copyright-template'
        mock_GetProjectRootDir.return_value = 'some/project/root/dir/'
        mock_ParseFileCopyright.return_value = ({1: '# =some-copyright-line'}, {2: '# =some-signature-line'})
        mock_open = mock.mock_open(read_data="# =some-file-content")
        mock_CheckCopyrightFormat.return_value = 0
        mock_CheckCopyrightSignature.return_value = 0

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        with mock.patch('builtins.open', mock_open):
            result = CopyrightCheck.CheckCopyrightForFiles(['some-file.java', 'another-file.java'], {'@address.com': 'Some Company'}, '@address.com')
        sys.stdout = sys.__stdout__
        self.assertEqual(result, 0)
        self.assertEqual(capturedOutput.getvalue(), "")

        mock_GetCopyrightTemplate.assert_called_once_with()
        mock_GetProjectRootDir.assert_called_once_with()
        self.assertEqual(mock_ParseFileCopyright.call_count, 2)
        mock_CheckCopyrightFormat.assert_has_calls([
            mock.call({1: '=some-copyright-line'}, 'some-copyright-template', 'some/project/root/dir/some-file.java'),
            mock.call({1: '=some-copyright-line'}, 'some-copyright-template', 'some/project/root/dir/another-file.java')
        ])
        mock_CheckCopyrightSignature.assert_has_calls([
            mock.call({2: '# =some-signature-line'}, 'Some Company', 'some/project/root/dir/some-file.java'),
            mock.call({2: '# =some-signature-line'}, 'Some Company', 'some/project/root/dir/another-file.java')
        ])
        self.assertEqual(mock_CheckCopyrightFormat.call_count, 2)
        self.assertEqual(mock_CheckCopyrightSignature.call_count, 2)


    def test_CheckCopyrightFormatWhichIsWrong(self):
        fileCopyright = {1: '---LICENSE_START---\n',
                         2: 'This is the license typo\n',
                         3: '',
                         4: '===license_end===\n'}
        templateCopyright = ['===LICENSE_START===\n',
                             'This is the license\n',
                             '\n',
                             '===LICENSE_END===\n']

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        result = CopyrightCheck.CheckCopyrightFormat(fileCopyright, templateCopyright, 'some/file/path')
        sys.stdout = sys.__stdout__

        expectedOutput = ("some/file/path | line  1 read \t  '---LICENSE_START---\\n'\n" +
                          "some/file/path | line  1 expected '===LICENSE_START===\\n'\n" +
                          "some/file/path | line  2 read \t  'This is the license typo\\n'\n" +
                          "some/file/path | line  2 expected 'This is the license\\n'\n" +
                          "some/file/path | line  3 read \t  ''\n" +
                          "some/file/path | line  3 expected '\\n'\n" +
                          "some/file/path | line  4 read \t  '===license_end===\\n'\n" +
                          "some/file/path | line  4 expected '===LICENSE_END===\\n'\n")

        self.assertEqual(capturedOutput.getvalue(), expectedOutput)
        self.assertEqual(result, 4)

    def test_CheckCopyrightFormatWhichIsCorrect(self):
        fileCopyright = {1: '===LICENSE_START===\n',
                         2: 'This is the license\n',
                         3: '\n',
                         4: '===LICENSE_END===\n'}
        templateCopyright = ['===LICENSE_START===\n',
                             'This is the license\n',
                             '\n',
                             '===LICENSE_END===\n']

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        result = CopyrightCheck.CheckCopyrightFormat(fileCopyright, templateCopyright, 'some/file/path')
        sys.stdout = sys.__stdout__

        self.assertEqual(capturedOutput.getvalue(), "")
        self.assertEqual(result, 0)

    def test_CheckCopyrightSignatureWhichIsWrong(self):
        fileSignatures = {1: "Trigger expected Copy-right",
                          2: "Trigger expected Mod Copy-right"}
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        result = CopyrightCheck.CheckCopyrightSignature(fileSignatures, 'Some-Company', 'some/file/path')
        sys.stdout = sys.__stdout__

        expectedOutput = ("some/file/path | line 1 expected Copyright\n" +
                          "some/file/path | line 2 expected Modifications Copyright\n" +
                          "some/file/path | missing company name and year for Some-Company\n")

        self.assertEqual(capturedOutput.getvalue(), expectedOutput)
        self.assertEqual(result, 3)

    def test_CheckCopyrightSignatureWhichHasWrongYear(self):
        currentYear = datetime.date.today().year
        fileSignatures = {1: "Copyright (C) 1999 Some-Company"}

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        result = CopyrightCheck.CheckCopyrightSignature(fileSignatures, 'Some-Company', 'some/file/path')
        sys.stdout = sys.__stdout__

        self.assertEqual(capturedOutput.getvalue(),
                         "some/file/path | line 1 update year to include " + str(currentYear) + "\n")
        self.assertEqual(result, 1)

    def test_CheckCopyrightSignatureWhichIsRight(self):
        currentYear = datetime.date.today().year
        fileSignatures = {1: "Copyright (C) " + str(currentYear) + " Some-Company"}

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout
        result = CopyrightCheck.CheckCopyrightSignature(fileSignatures, 'Some-Company', 'some/file/path')
        sys.stdout = sys.__stdout__

        self.assertEqual(capturedOutput.getvalue(), "")
        self.assertEqual(result, 0)

    @mock.patch('CopyrightCheck.CheckCopyrightForFiles')
    @mock.patch('CopyrightCheck.FindAlteredFiles')
    @mock.patch('CopyrightCheck.CheckCommitterInConfigFile')
    @mock.patch('CopyrightCheck.ReadProjectCommittersConfigFile')
    @mock.patch('CopyrightCheck.GetCommitterEmailExtension')
    @mock.patch('CopyrightCheck.PermissionsCheck')
    def test_Main(self, mock_PermissionsCheck, mock_GetCommitterEmailExtension, mock_ReadProjectCommittersConfigFile,
                  mock_CheckCommitterInConfigFile, mock_FindAlteredFiles, mock_CheckCopyrightForFiles):

        mock_GetCommitterEmailExtension.return_value = '@address.com'
        mock_ReadProjectCommittersConfigFile.return_value = {'@address.com', 'Some Company'}
        mock_FindAlteredFiles.return_value = ['some-file.java']
        mock_CheckCopyrightForFiles.return_value = 5

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout

        CopyrightCheck.main()

        sys.stdout = sys.__stdout__

        expectedOutput = (BANNER + '\nCopyright Check Python Script:\n' +
                          '5 issue(s) found after 1 altered file(s) checked\n' +
                          BANNER + '\n')

        self.assertEqual(capturedOutput.getvalue(), expectedOutput)

        mock_PermissionsCheck.assert_called_once_with()
        mock_GetCommitterEmailExtension.assert_called_once_with()
        mock_ReadProjectCommittersConfigFile.assert_called_once_with()
        mock_CheckCommitterInConfigFile.assert_called_once_with('@address.com', {'@address.com', 'Some Company'})
        mock_FindAlteredFiles.assert_called_once_with()
        mock_CheckCopyrightForFiles.assert_called_once_with(['some-file.java'], {'@address.com', 'Some Company'}, '@address.com')

    @mock.patch('CopyrightCheck.CheckCopyrightForFiles')
    @mock.patch('CopyrightCheck.FindAlteredFiles')
    @mock.patch('CopyrightCheck.CheckCommitterInConfigFile')
    @mock.patch('CopyrightCheck.ReadProjectCommittersConfigFile')
    @mock.patch('CopyrightCheck.GetCommitterEmailExtension')
    @mock.patch('CopyrightCheck.PermissionsCheck')
    def test_MainNoFiles(self, mock_PermissionsCheck, mock_GetCommitterEmailExtension, mock_ReadProjectCommittersConfigFile,
                  mock_CheckCommitterInConfigFile, mock_FindAlteredFiles, mock_CheckCopyrightForFiles):

        mock_GetCommitterEmailExtension.return_value = '@address.com'
        mock_ReadProjectCommittersConfigFile.return_value = {'@address.com', 'Some Company'}
        mock_FindAlteredFiles.return_value = []

        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput  # Capture output to stdout

        CopyrightCheck.main()

        sys.stdout = sys.__stdout__

        expectedOutput = (BANNER + '\nCopyright Check Python Script:\n' +
                          '0 issue(s) found after 0 altered file(s) checked\n' +
                          BANNER + '\n')

        self.assertEqual(capturedOutput.getvalue(), expectedOutput)

        mock_PermissionsCheck.assert_called_once_with()
        mock_GetCommitterEmailExtension.assert_called_once_with()
        mock_ReadProjectCommittersConfigFile.assert_called_once_with()
        mock_CheckCommitterInConfigFile.assert_called_once_with('@address.com', {'@address.com', 'Some Company'})
        mock_FindAlteredFiles.assert_called_once_with()
        mock_CheckCopyrightForFiles.assert_not_called()


if __name__ == '__main__':
    unittest.main()
