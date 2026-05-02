@echo off
title eFacturacao - HZ Consultoria
echo Iniciando o Sistema eFacturacao...
echo Por favor, nao feche esta janela enquanto estiver a usar o sistema.
echo -------------------------------------------------------------------
echo Desenvolvido por: HZ Consultoria
echo -------------------------------------------------------------------

start "" http://localhost:8080
java -jar efacturacao-0.0.1-SNAPSHOT.war

pause
