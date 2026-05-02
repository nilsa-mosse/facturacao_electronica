[Setup]
AppName=eFacturacao
AppVersion=2.0
DefaultDirName={pf}\eFacturacao
DefaultGroupName=eFacturacao
UninstallDisplayIcon={app}\efacturacao.exe
Compression=lzma2
SolidCompression=yes
OutputDir=userdocs:Inno Setup Examples Output
OutputBaseFilename=eFacturacao_Setup_HZ
AppPublisher=HZ Consultoria
AppPublisherURL=https://www.hzconsultoria.co.ao
; Mostra informação sobre a HZ Consultoria no início
InfoBeforeFile=d:\eclipse_workspace\facturacao_electronica\src\main\resources\static\INFO_HZ.txt
; SetupIconFile=d:\eclipse_workspace\facturacao_electronica\src\main\resources\static\img\logo.png

[Files]
; O WAR/JAR do sistema (assume-se que foi gerado via mvn package)
Source: "d:\eclipse_workspace\facturacao_electronica\target\efacturacao-0.0.1-SNAPSHOT.war"; DestDir: "{app}"; Flags: ignoreversion
; Launcher em Batch
Source: "d:\eclipse_workspace\facturacao_electronica\efacturacao_launcher.bat"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\eFacturacao"; Filename: "{app}\efacturacao_launcher.bat"
Name: "{commondesktop}\eFacturacao"; Filename: "{app}\efacturacao_launcher.bat"

[Run]
Filename: "{app}\efacturacao_launcher.bat"; Description: "Lançar eFacturacao"; Flags: postinstall nowait
