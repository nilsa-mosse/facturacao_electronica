@echo off
setlocal enabledelayedexpansion

title Kwanza ERP - HZ Consultoria
echo ===================================================================
echo              INICIANDO O SISTEMA KWANZA ERP
echo ===================================================================
echo.
echo Por favor, nao feche esta janela enquanto estiver a usar o sistema.
echo -------------------------------------------------------------------

:: Definir o caminho do Java
set JAVA_EXEC=java
if exist "%~dp0jre\bin\java.exe" (
    set JAVA_EXEC="%~dp0jre\bin\java.exe"
    echo [INFO] Usando JRE incorporado.
) else (
    echo [INFO] Usando Java do sistema.
)

:: Verificar se o arquivo do sistema existe
set WAR_FILE=%~dp0efacturacao-0.0.1-SNAPSHOT.war
if not exist "!WAR_FILE!" (
    echo [ERRO] Arquivo do sistema nao encontrado: efacturacao-0.0.1-SNAPSHOT.war
    echo Certifique-se de que o arquivo esta na pasta: %~dp0
    pause
    exit /b
)

:: Iniciar o servidor em segundo plano
echo [INFO] Iniciando o servidor... (Pode levar ate 30 segundos)
:: Otimizacao JVM: Xms (memoria inicial), Xmx (memoria maxima), UseG1GC (coletor de lixo moderno)
start /b "" %JAVA_EXEC% -Xms512m -Xmx1024m -XX:+UseG1GC -XX:+ParallelRefProcEnabled -jar "!WAR_FILE!"

:: Aguardar o servidor ficar pronto na porta 8080
echo [INFO] Aguardando inicializacao na porta 8080...
set "ready=0"
for /L %%i in (1,1,40) do (
    netstat -ano | findstr :8080 > nul
    if !errorlevel! equ 0 (
        set "ready=1"
        goto :launch
    )
    echo Tentativa %%i de 40... Aguardando servidor...
    timeout /t 2 /nobreak > nul
)

:launch
if "!ready!"=="1" (
    echo [SUCESSO] Servidor pronto! Abrindo o sistema...
    start "" http://localhost:8080/login
) else (
    echo.
    echo [AVISO] O servidor esta demorando muito para responder.
    echo Tentando abrir o navegador assim mesmo...
    start "" http://localhost:8080/login
    echo Se vir um erro de "Conexao Recusada", aguarde mais alguns segundos e atualize o navegador (F5).
)

echo.
echo -------------------------------------------------------------------
echo Sistema em execucao. Minimize esta janela, mas NAO a feche.
echo Para encerrar o sistema, feche esta janela ou pressione Ctrl+C.
echo -------------------------------------------------------------------
pause

