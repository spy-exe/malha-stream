# Testes

## Automatizados

```bash
./gradlew --no-daemon --max-workers=2 :telemetry:check
python3 -m unittest discover -s tests -v
python3 -m coverage run --source=benchmark.analysis,benchmark.events,benchmark.pipeline -m unittest discover -s tests -v
python3 -m coverage report --fail-under=85
./gradlew --no-daemon --max-workers=2 :app:lintNonRootDebug :app:assembleNonRootDebug :app:assembleNonRootRelease :app:assembleNonRootDebugAndroidTest
```

Java puro: limites do ring, exportação CSV, snapshots, pareamento de timestamps locais, missing data, amostras inválidas, percentis e política de decoder opt-in. Python: clocks da câmera, diferenças relativas assinadas, amostras insuficientes para caudas, comparabilidade, regressões e plano intercalado. Os números nos testes são **fixtures sintéticos de unidade**, nunca resultados de desempenho.

A cobertura mínima é 85% do núcleo novo testável: `telemetry/`, `benchmark.analysis`, `benchmark.events` e `benchmark.pipeline`. Bindings Android, APIs de driver/MediaCodec e comandos externos de ADB/câmera são excluídos dessa conta; exigem integração no dispositivo. Essa exclusão não significa que o app inteiro tenha 85% de cobertura.

Os commits TDD e runs de falha inicial foram preservados. O upstream inspecionado não tinha suíte JVM Android equivalente adicionada ao projeto; build/lint originais são o baseline automatizado relevante.

## Dispositivo real

Não habilitar depuração sem autorização. Em dispositivo do usuário já autorizado:

```bash
adb connect IP_DA_TV:5555
adb -s IP_DA_TV:5555 devices
adb -s IP_DA_TV:5555 install -r artifacts/malha-debug.apk
adb -s IP_DA_TV:5555 install -r artifacts/malha-android-test.apk
adb -s IP_DA_TV:5555 shell am instrument -w app.malhastream.debug.test/androidx.test.runner.AndroidJUnitRunner
```

O applicationId real do test APK pode conter flavor/suffix: use `aapt dump badging` para obter o pacote de instrumentação antes do último comando. Os testes de integração checam instalação paralela, launcher TV e proteção do provider; **ainda precisam ser executados em hardware**.

Checklist manual: D-pad e remoto; pairing Sunshine; H.264 e HEVC; imagem/áudio; Bluetooth e USB; botões e vibração; reconnect; perda/recuperação de rede controlada no dispositivo de teste; background/foreground; resolução; sessão prolongada e temperaturas. Preserve configurações equivalentes e registre cada execução como PASS/FAIL/NOT_RUN. Não simular aprovação com emulador.

O protocolo físico, amostragem e critérios ficam em [PERFORMANCE.md](PERFORMANCE.md). Nenhum teste automatizado neste arquivo prova input-to-photon.
