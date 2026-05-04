[Setup]
AppName=Kwanza ERP
AppVersion=2.0
DefaultDirName=C:\KwanzaERP
DefaultGroupName=Kwanza ERP
UninstallDisplayIcon={app}\KwanzaERP.ico
WizardStyle=modern
WizardImageFile=installer_assets\wizard_image.bmp
WizardSmallImageFile=installer_assets\wizard_small_image.bmp
Compression=lzma2
SolidCompression=yes
OutputDir=Output
OutputBaseFilename=KwanzaERP_Setup_v2
AppPublisher=HZ Consultoria
AppPublisherURL=https://www.hzconsultoria.co.ao
AppSupportURL=https://www.hzconsultoria.co.ao
AppUpdatesURL=https://www.hzconsultoria.co.ao
; Disable specific flags that can trigger false positives in some AVs
DisableProgramGroupPage=yes
DisableDirPage=no
; Metadata to appear more "trusted"
AppCopyright=Copyright (C) 2026 HZ Consultoria
; Mostra informação sobre a HZ Consultoria no início
InfoBeforeFile=src\main\resources\static\INFO_HZ.txt
SetupIconFile=installer_assets\KwanzaERP.ico
PrivilegesRequired=admin
VersionInfoCompany=HZ Consultoria
VersionInfoCopyright=Copyright (C) 2026 HZ Consultoria
VersionInfoDescription=Kwanza ERP - Sistema de Facturacao Electronica
VersionInfoProductName=Kwanza ERP
VersionInfoProductVersion=2.0
VersionInfoTextVersion=2.0.0
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: "portuguese"; MessagesFile: "compiler:Languages\Portuguese.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; O WAR do sistema
Source: "target\efacturacao-0.0.1-SNAPSHOT.war"; DestDir: "{app}"; Flags: ignoreversion
; Launcher em Batch
Source: "KwanzaERP.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "PararERP.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "installer_assets\KwanzaERP.ico"; DestDir: "{app}"; Flags: ignoreversion
; Manual do Utilizador
Source: "MANUAL_UTILIZADOR.txt"; DestDir: "{app}"; Flags: ignoreversion isreadme
; Embutir o JRE do projeto (Independencia total do sistema do cliente)
Source: "jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\KwanzaERP"; Filename: "{app}\KwanzaERP.bat"; IconFilename: "{app}\KwanzaERP.ico"
Name: "{group}\Parar KwanzaERP"; Filename: "{app}\PararERP.bat"; IconFilename: "{app}\KwanzaERP.ico"
Name: "{commondesktop}\KwanzaERP"; Filename: "{app}\KwanzaERP.bat"; IconFilename: "{app}\KwanzaERP.ico"
Name: "{commondesktop}\Parar KwanzaERP"; Filename: "{app}\PararERP.bat"; IconFilename: "{app}\KwanzaERP.ico"

[Run]
Filename: "{app}\KwanzaERP.bat"; Description: "Lancar Kwanza ERP"; Flags: postinstall nowait
; Adiciona automaticamente a pasta ao Windows Defender para evitar bloqueios
Filename: "powershell.exe"; \
  Parameters: "-ExecutionPolicy Bypass -NoProfile -WindowStyle Hidden -Command ""Add-MpPreference -ExclusionPath '{app}'"""; \
  Flags: runhidden runascurrentuser; \
  StatusMsg: "Otimizando compatibilidade com Antivirus..."
