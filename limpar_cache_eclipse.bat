@echo off
REM Script para limpar cache do Eclipse e sincronizar com Maven
REM Execute este script com o Eclipse FECHADO

echo ============================================
echo Correcao de Erros Eclipse - JUnit Imports
echo ============================================
echo.

setlocal enabledelayedexpansion

REM Verificar se o Eclipse está rodando
tasklist /FI "IMAGENAME eq eclipse.exe" 2>NUL | find /I /N "eclipse.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [ERRO] Eclipse ainda está aberto!
    echo Por favor, feche o Eclipse antes de executar este script.
    echo.
    pause
    exit /b 1
)

echo [OK] Eclipse não está em execução
echo.

REM Mudar para o diretório do workspace
cd /d "D:\eclipse_workspace"

if not exist ".metadata" (
    echo [INFO] Pasta .metadata não encontrada
    goto MAVEN_SYNC
)

echo Limpando cache do Eclipse...
echo.

REM Remover caches do JDT
echo Removendo cache JDT...
if exist ".metadata\plugins\org.eclipse.jdt.core" (
    rmdir /s /q ".metadata\plugins\org.eclipse.jdt.core" 2>nul
    echo [OK] Cache JDT removido
) else (
    echo [INFO] Cache JDT não encontrado
)

REM Remover cache de recursos
echo Removendo cache de recursos...
if exist ".metadata\plugins\org.eclipse.core.resources" (
    rmdir /s /q ".metadata\plugins\org.eclipse.core.resources" 2>nul
    echo [OK] Cache de recursos removido
) else (
    echo [INFO] Cache de recursos não encontrado
)

REM Remover cache do M2E
echo Removendo cache M2E...
if exist ".metadata\plugins\org.eclipse.m2e.core" (
    rmdir /s /q ".metadata\plugins\org.eclipse.m2e.core" 2>nul
    echo [OK] Cache M2E removido
) else (
    echo [INFO] Cache M2E não encontrado
)

echo.

:MAVEN_SYNC
echo Sincronizando com Maven...
echo.

cd /d "D:\eclipse_workspace\facturacao_electronica"

REM Executar Maven clean install
call mvn.cmd clean install -DskipTests -q

if "%ERRORLEVEL%"=="0" (
    echo.
    echo [OK] Maven sincronizou com sucesso!
) else (
    echo.
    echo [ERRO] Erro ao executar Maven
    pause
    exit /b 1
)

echo.
echo ============================================
echo [CONCLUIDO] Cache limpo e Maven sincronizado
echo ============================================
echo.
echo Agora:
echo 1. Abra o Eclipse
echo 2. Espere o indexing terminar
echo 3. Os erros de imports JUnit devem desaparecer
echo.
pause
