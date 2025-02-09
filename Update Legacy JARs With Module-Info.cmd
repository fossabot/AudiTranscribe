@echo off
SETLOCAL EnableDelayedExpansion

:: Setup
ECHO Please run the following command in a separate terminal. You will need to enter its output in a while.
ECHO     mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout

pause

:: Constants
SET GROUP_IDS[1]=org.apache.commons
SET GROUP_IDS[2]=org.apache.commons
SET GROUP_IDS[3]=org.javatuples
SET GROUP_IDS[4]=org.xerial

SET ARTIFACT_IDS[1]=commons-compress
SET ARTIFACT_IDS[2]=commons-exec
SET ARTIFACT_IDS[3]=javatuples
SET ARTIFACT_IDS[4]=sqlite-jdbc

SET MODULE_NAMES[1]=org.apache.commons.compress
SET MODULE_NAMES[2]=commons.exec
SET MODULE_NAMES[3]=javatuples
SET MODULE_NAMES[4]=org.xerial.sqlitejdbc

SET MODULE_VERSIONS[1]=1.21
SET MODULE_VERSIONS[2]=1.3
SET MODULE_VERSIONS[3]=1.2
SET MODULE_VERSIONS[4]=3.36.0.3

SET MODULE_JARS[1]=commons-compress-1.21.jar
SET MODULE_JARS[2]=commons-exec-1.3.jar
SET MODULE_JARS[3]=javatuples-1.2.jar
SET MODULE_JARS[4]=sqlite-jdbc-3.36.0.3.jar

:: Get local Maven repository
SET /P repositoryDir="Enter output of the above command: "

:: Iterate through each one of the problematic modules
FOR /L %%i IN (1,1,4) DO (
    :: Get the module name and version
    SET groupID=!GROUP_IDS[%%i]!
    SET artifactID=!ARTIFACT_IDS[%%i]!
    SET moduleName=!MODULE_NAMES[%%i]!
    SET moduleVersion=!MODULE_VERSIONS[%%i]!
    SET moduleJar=!MODULE_JARS[%%i]!

    :: Get the module's JAR file
    SET moduleDir=!repositoryDir!\!groupID:.=\!\!artifactID!\!moduleVersion!
    SET moduleJarPath=!moduleDir!\!moduleJar!

    :: Navigate into that directory
    cd !moduleDir!

    :: Generate the module-info.java file
    jdeps --generate-module-info . !moduleJar! | find /I "Missing dependencies"

    :: Error level 0 means that "Missing dependencies" was found
    IF %ERRORLEVEL% == 0 (
        :: Try again, but this time ignoring dependencies
        jdeps --ignore-missing-deps --generate-module-info . !moduleJar!

        IF %ERRORLEVEL% GEQ 1 (
          :: If we still can't generate the module-info.java file, then we're done
          echo Failed to generate module-info.java for !artifactID!, !moduleVersion!
          pause
          exit 1
        )
    )

    :: Patch module
    javac --patch-module !moduleName!=!moduleJar! !moduleName!\module-info.java

    :: Recompile jar
    jar uf !moduleJar! -C !moduleName! module-info.class

    :: Report that we have done the updating
    ECHO Updated !artifactID!, !moduleVersion!
)
