# Fire TV AFTSSS: verificação inicial

Estado: **DEVICE VERIFIED**, limitado à instalação, inicialização da interface e dois testes de integração de runtime. **BENCHMARKED** e **IMPROVEMENT VERIFIED** permanecem pendentes. Input-to-photon: zero eventos medidos; ganho indisponível.

## Identificação e integridade

O dispositivo autorizou a chave ADB e respondeu em `10.10.1.2:5555`. O endereço inicialmente informado, `10.1.1.2`, não respondeu. `Build.MODEL=AFTSSS`, device `sheldonp`, fabricante Amazon, Android 9/API 28, `ro.board.platform=mt8695`, display lógico/físico informado por `wm size` de 1920×1080.

A [tabela oficial Amazon](https://developer.amazon.com/docs/device-specs/identify-fire-tv-devices.html) associa AFTSSS ao Fire TV Stick de terceira geração (2020), Fire OS 7. Essa identificação não mede capacidade sustentada, temperatura ou latência.

A release já estava instalada pelo usuário, e sua atividade `Game` foi observada antes da coleta. Isso não comprova qualidade da imagem, áudio, input ou duração de streaming. Debug e APK de teste foram instalados em paralelo; os três APKs instalados têm SHA-256 idêntico aos arquivos publicados da versão 0.1.0-dev, source `695fa8f782264017a4203aad62d46e6b683ebfbe`.

## Testes executados

- Instalação debug/test via ADB: PASS.
- Dois testes Android de identidade paralela/launcher TV e provider privado, rejeitando escrita e path traversal: PASS.
- Interface debug `PcView` lançada e conferida por captura de tela privada: PASS.
- Descoberta do Sunshine informado: PASS; novo pareamento debug solicitado, sem iniciar jogo.
- D-pad completo, imagem/áudio no debug, gamepad, recovery de rede e sessão prolongada: NOT_RUN.

O teste de launcher resolve o intent; a inicialização da interface foi conferida separadamente. Captura de framebuffer comprova a interface capturada pelo Android, não o instante em que pixels aparecem no painel.

## Decoder

O XML do driver declara `OMX.MTK.VIDEO.DECODER.AVC` e `OMX.MTK.VIDEO.DECODER.HEVC`, com tamanho máximo 1920×1088 e faixa de 1–60 FPS. São declarações estáticas; o decoder efetivamente selecionado e seu desempenho ainda precisam de sessão instrumentada. A API de consulta de `FEATURE_LowLatency` não existe no API 28; suporte de fabricante não foi inferido. O experimento MiTV/Amlogic/API34 não se aplica e permanece desligado.

## Próximo ciclo

O debug foi configurado para H.264, 1920×1080, 60 FPS, 20 Mbps, HDR desligado, pacing upstream de menor latência e gravação habilitada. Isso é preparação de coleta, não resultado de desempenho. Concluir o pareamento no Sunshine, executar a mesma cena por ≥120 segundos, encerrar normalmente e coletar JSON/CSV. Depois realizar A/B1/B2/C com condições equivalentes e câmera/LED; nenhuma otimização foi aprovada com os dados desta verificação.

[Metadados e hashes](evidence/firetv-AFTSSS-verification.json) · [Log dos testes reais](evidence/firetv-AFTSSS-instrumentation.log). PIN, credenciais, certificados privados e capturas da interface não foram publicados.
