@rem ----------------------------------------------------------------------------
@rem ����Amoeba�Ľű�
@rem
@rem ��Ҫ�������»���������
@rem
@rem    JAVA_HOME           - JDK�İ�װ·��
@rem
@rem ----------------------------------------------------------------------------
@echo off
if "%OS%"=="Windows_NT" setlocal

:CHECK_JAVA_HOME
if not "%JAVA_HOME%"=="" goto SET_AMOEBA_HOME

echo.
echo ����: �������û���������JAVA_HOME����ָ��JDK�İ�װ·��
echo.
goto END

:SET_AMOEBA_HOME
set AMOEBA_HOME=%~dp0..
if not "%AMOEBA_HOME%"=="" goto START_AMOEBA

echo.
echo ����: �������û���������AMOEBA_HOME����ָ��Amoeba�İ�װ·��
echo.
goto END

:START_AMOEBA

set DEFAULT_OPTS=-server -Xms256m -Xmx256m -Xss128k
set DEFAULT_OPTS=%DEFAULT_OPTS% -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+UseParallelGC -XX:+UseBiasedLocking -XX:NewSize=64m
set DEFAULT_OPTS=%DEFAULT_OPTS% "-Damoeba.home=%AMOEBA_HOME%"
set DEFAULT_OPTS=%DEFAULT_OPTS% "-Dclassworlds.conf=%AMOEBA_HOME%\bin\amoeba.classworlds"

set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
set CLASSPATH="%AMOEBA_HOME%\lib\classworlds-1.0.jar"
set MAIN_CLASS="org.codehaus.classworlds.Launcher"

%JAVA_EXE% %DEFAULT_OPTS% -classpath %CLASSPATH% %MAIN_CLASS% %*

:END
if "%OS%"=="Windows_NT" endlocal
pause