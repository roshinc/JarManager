# Jar Manager Command-Line Utility

This command-line utility provides functionality to manage JAR files and generate various files related to them.

#### Usage

```shell
jarmanager [command] [options] [arguments]
```

#### Commands

### 1. Generate POM Entries

```shell
jarmanager generate-pom [options] <folder_path> <output_file_path>
```

* `<folder_path>`: Path to the folder containing the JAR files.
* `<output_file_path>`: Path to the output XML file that will contain the POM entries.

Options:

* `--config <config_file_path>`: Specify the path to the configuration file (optional).
* `--generate-artifact-list`: Generate an additional text file with the groupId:artifactName:version for all the JARs (
  optional).
* `--email-friendly-format`: When used in conjunction with --generate-artifact-list, entries in the additional text file
  are formatted in a more readable, email-friendly manner. This format includes details such as group ID, artifact ID,
  version, and a URL to the artifact. Each entry is separated by a separator to enhance clarity when viewed
  in an email (optional).

Notes:

* Source JARs are not included in the POM entries. If there is source JAR is present in the folder, a warning will be
  displayed. and it will be skipped.
* If there are JARs with no pom information, a warning will be displayed per JAR, and the jar will be skipped.

### 2. Download JARs

```shell
jarmanager download [options] <input_file_path> <target_folder_path>
```

* `<input_file_path>`: Path to the file containing the groupId:artifactName:version entries.
* `<target_folder_path>`: Path to the target folder where the downloaded JARs will be saved.

Options:

* `--config <config_file_path>`: Specify the path to the configuration file (optional).
* `--source-folder`: <source_folder_path>: Specify the target folder path for downloading source JARs (optional).
* `--update-different-only`: Only replace the JARs that have a different version (optional).
* `--changes-file <changes_file_path>`: Specify the path to the changes text file that will be appended with the changes
  (optional).
* `--use-remote-name` : Use the remote file name for the downloaded file (optional).

### 3. Generate User Libraries XML

```shell
jarmanager generate-userlibs [options] <output_file_path>
```

* `<output_file_path>`: Path to the output XML file that will contain the user libraries entries.

Options:

* `--config <config_file_path>`: Specify the path to the configuration file (optional).
* `--download`: Download missing JARs (optional).
  `--dont-overwrite`: Don't overwrite existing JARs during download (only applicable if --download is set) (optional).
  `--shared-libs <shared_libs_file_path>`: Path to the text file listing shared libraries (see format below) (optional).

Shared Libraries File Format:

```text 
<shared_lib_name_1>;<shared_lib_file_path_1>;<jar_target_path_1>;<jar_source_target_path_1>
<shared_lib_name_2>;<shared_lib_file_path_2>;<jar_target_path_2>;<jar_source_target_path_2>
...
```

### Configuration File

All commands accept an optional --config flag to specify the path to a configuration file. The configuration file can
contain default values for various options and paths.

Example Configuration File:

```properties
folder_path=/path/to/jar/folder
output_file_path=/path/to/output/file.xml
source_folder_path=/path/to/source/folder
changes_file_path=/path/to/changes/file.txt
shared_libs_file_path=/path/to/shared/libs/file.txt
```

Example Usage:

1. Generate POM entries for JARs in a folder, and also generate an artifact list:

```shell
jarmanager generate-pom /path/to/jar/folder /path/to/output/file.xml --generate-artifact-list
```

1. Generate POM entries for JARs in a folder, and also generate an email-friendly artifact list:

```shell
jarmanager generate-pom /path/to/jar/folder /path/to/output/file.xml --generate-artifact-list --email-friendly-format
```

3. Download JARs listed in a file:

```shell
jarmanager download /path/to/input/file.txt /path/to/target/folder --source-folder /path/to/source/folder --update-different-only --changes-file /path/to/changes/file.txt
```

4. Generate user libraries XML:

```shell
jarmanager generate-userlibs /path/to/output/userlibs.xml --download --dont-overwrite --shared-libs /path/to/shared/libs/file.txt
```
