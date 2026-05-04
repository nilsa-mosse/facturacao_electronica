@echo off
title Parar Kwanza ERP
echo ===================================================================
echo              ENCERRANDO O SISTEMA KWANZA ERP
echo ===================================================================
echo.

:: Procurar o processo Java que esta rodando o efacturacao
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    set PID=%%a
)

if defined PID (
    echo [INFO] Encontrado servidor Kwanza ERP (PID: %PID%).
    echo [INFO] Encerrando o processo...
    taskkill /PID %PID% /F > nul 2>&1
    if %errorlevel% equ 0 (
        echo [SUCESSO] Sistema encerrado com sucesso.
    ) else (
        echo [ERRO] Nao foi possivel encerrar o processo. Tente como Administrador.
    )
) else (
    echo [AVISO] Nao foi encontrado nenhum servidor rodando na porta 8080.
)

echo.
echo -------------------------------------------------------------------
pause
