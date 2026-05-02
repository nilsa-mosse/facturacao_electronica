[Setup]
AppName=eFacturacao
AppVersion=2.0
DefaultDirName={pf}\eFacturacao
DefaultGroupName=eFacturacao
UninstallDisplayIcon={app}\efacturacao.exe
WizardStyle=modern
WizardImageFile=d:\eclipse_workspace\facturacao_electronica\installer_assets\sidebar.bmp
WizardSmallImageFile=d:\eclipse_workspace\facturacao_electronica\installer_assets\small_logo.bmp
Compression=lzma2
SolidCompression=yes
OutputDir=d:\eclipse_workspace\facturacao_electronica\dist
OutputBaseFilename=eFacturacao_Setup_HZ
AppPublisher=HZ Consultoria
AppPublisherURL=https://www.hzconsultoria.co.ao
; Mostra informação sobre a HZ Consultoria no início
InfoBeforeFile=d:\eclipse_workspace\facturacao_electronica\src\main\resources\static\INFO_HZ.txt
; SetupIconFile=d:\eclipse_workspace\facturacao_electronica\src\main\resources\static\img\logo.png

[Languages]
Name: "portuguese"; MessagesFile: "compiler:Languages\Portuguese.isl"

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
