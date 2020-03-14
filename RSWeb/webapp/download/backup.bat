For /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set currentDate=%%c%%a%%b)
For /f "tokens=1-2 delims=/:" %%a in ("%TIME%") do (set currentTime=%%a%%b)

set CURRENT_TIMESTAMP=%currentDate%_%currentTime%
set SEVEN_Z="C:\Program Files\7-Zip\7z.exe"
set SOURCE_BAK=M:\test
set DEST_BAK=M:\Backups
set KEEP_DAY=30
set SCP_EXE="M:\Nick\OpenSSH\scp.exe"
set PRIV_KEY="M:\Nick\id_rsa"
set REMOTE_USER=remote
set REMOTE_IP=nick4ever.com
set REMOTE_DEST=/home/%REMOTE_USER%/buidsltd_backups
set JRE_HOME=M:\Nick\jre\bin

set BAK=%DEST_BAK%\BACKUP_%CURRENT_TIMESTAMP%

set EMAIL_NOTIFIER=M:\Nick\EmailNotifier.jar
set FROM_EMAIL=support@buidsltd.com
set FROM_EMAIL_MARKUP="Buids Ltd Support"
set FROM_EMAIL_PWD="!@#Support456"
set TO_EMAIL=vinh.pham@buidsltd.com
set SMTP_HOST=mail9020.maychuemail.com
set SMTP_PORT=587
set EMAIL_SUBJECT="Buids Ltd - Backup %CURRENT_TIMESTAMP%"

cd %DEST_BAK%

::xcopy %SOURCE_BAK% %BAK% /i /O /X /E /H /K
robocopy %SOURCE_BAK% %BAK% /E

%SEVEN_Z% a "%BAK%.7z" "%BAK%"

:: Rename bak folder due to long file name
::ren %BAK% A 
rmdir /Q /S %BAK%

::echo "Removing files"> %DEST_BAK%\log

::forfiles -p %DEST_BAK% -s -m *.7z -d -%KEEP_DAY% -c "cmd /c echo @path>> %DEST_BAK%\log"
forfiles -p %DEST_BAK% -s -m *.7z -d -%KEEP_DAY% -c "cmd /c del @path"

%SCP_EXE% -i %PRIV_KEY% %BAK%.7z %REMOTE_USER%@%REMOTE_IP%:%REMOTE_DEST%

for /f %%A in ("%BAK%.7z") do set BAK_SIZE=%%~zA
set /a FORMATED_SIZE=%BAK_SIZE%/1024/1024

set EMAIL_BODY="*** Buids Ltd - Schedule backup<br><br>Location: %DEST_BAK%<br>Filename: %DEST_BAK%\BACKUP_%CURRENT_TIMESTAMP%.7z<br>File size: %FORMATED_SIZE% GB"

%JRE_HOME%\java -jar %EMAIL_NOTIFIER% %FROM_EMAIL% %FROM_EMAIL_MARKUP% %FROM_EMAIL_PWD% %TO_EMAIL% %SMTP_HOST% %SMTP_PORT% %EMAIL_SUBJECT% %EMAIL_BODY%