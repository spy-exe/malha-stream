# Uso do Malha Stream

## Estado e arquivos entregues

O laboratório está **BUILD READY**. Nenhum Fire Stick/Android TV foi instalado ou testado nesta execução. **DEVICE VERIFIED**, **BENCHMARKED** e **IMPROVEMENT VERIFIED** permanecem pendentes; a medição física está **BLOCKED** por acesso autorizado ao dispositivo e instrumento externo. Ganho de input-to-photon é indisponível.

[Baixar 0.1.0-dev](https://github.com/spy-exe/malha-stream/releases/tag/v0.1.0-dev), compilada do commit `695fa8f782264017a4203aad62d46e6b683ebfbe`:

| Arquivo | Uso | Pacote |
|---|---|---|
| `malha-debug.apk` | Streaming com diagnóstico opcional, exportação e experimento manual | `app.malhastream.debug` |
| `malha-release.apk` | Uso normal, assinado; diagnóstico e experimento desativados | `app.malhastream.unofficial` |
| `malha-android-test.apk` | Testes de integração; instalar junto ao debug somente ao executar esses testes | `app.malhastream.debug.test` |

Debug e release podem coexistir com o Moonlight oficial. A assinatura debug do laboratório é persistente, mas difere potencialmente da usada pelo GitHub Actions; misturar APKs debug de builders diferentes pode exigir reinstalação e perde os dados locais quando o aplicativo é desinstalado. Prefira os APKs desta release para esta rodada.

## Acesso ao laboratório

CT **105**, hostname `malha-stream-build`, Debian 13, **10.10.1.88 por DHCP**, bridge `vmbr0`, disco `local-lvm` de 32 GiB, 2 vCPU, 4 GiB RAM e 1 GiB swap. É unprivileged, sem nesting; inicia com o PVE. O usuário `malha` não tem sudo.

No terminal do PVE, a chave SSH já autorizada permite:

```bash
ssh malha@10.10.1.88
cd /home/malha/malha-stream
source /etc/profile.d/malha-android.sh
```

De outro computador, use primeiro seu acesso habitual ao PVE; a chave desse computador não foi adicionada diretamente ao CT. Se o DHCP mudar o endereço, consulte no PVE `pct exec 105 -- hostname -I`.

Os APKs publicados também estão em `/home/malha/malha-stream/artifacts`. Confira-os no CT:

```bash
cd /home/malha/malha-stream/artifacts
sha256sum -c SHA256SUMS
cd /home/malha/malha-stream
```

## Instalar no Fire TV ou Android TV

Descubra o IP da TV na tela de rede do próprio dispositivo. No Fire TV, o proprietário pode habilitar **Configurações → Meu Fire TV → Opções do desenvolvedor → Depuração ADB**. Em versões com menu oculto, selecione o nome do dispositivo sete vezes em **Sobre**. Veja o [procedimento oficial Amazon](https://developer.amazon.com/docs/fire-tv/connecting-adb-to-device.html). No Android TV, o caminho depende do fabricante; use a conexão de depuração oferecida pelo aparelho. Não abra portas no roteador.

No CT, substitua `IP_DA_TV:5555` pelo endereço ADB efetivo. A porta 5555 é o caso comum do Fire TV; Android com depuração sem fio pode exigir `adb pair IP:PORTA` e outra porta de conexão mostrada na TV.

```bash
adb connect IP_DA_TV:5555
adb devices -l
```

Aceite a autorização **na sua TV**. Prossiga somente se ela aparecer com estado `device`; `unauthorized` exige aceitar a chave, e `offline` ainda não é conexão utilizável.

Para diagnóstico:

```bash
adb -s IP_DA_TV:5555 install -r artifacts/malha-debug.apk
adb -s IP_DA_TV:5555 shell am start -n app.malhastream.debug/com.limelight.PcView
```

Para uso normal, escolha a release:

```bash
adb -s IP_DA_TV:5555 install -r artifacts/malha-release.apk
adb -s IP_DA_TV:5555 shell am start -n app.malhastream.unofficial/com.limelight.PcView
```

O laboratório compila e coleta os dados. O APK roda na TV; o CT não retransmite o vídeo nem substitui o Sunshine.

## Parear e jogar

Abra Malha Stream pelo launcher da TV, usando o controle remoto. Se o PC não aparecer automaticamente, adicione **10.10.1.1**. Selecione o PC e confirme o PIN exibido pelo cliente no painel do Sunshine que já existe no PC. Abra a mesma aplicação/jogo que usa no Moonlight. O Sunshine foi encontrado ocupado durante esta execução; nenhum jogo ou serviço foi interrompido.

Conecte o gamepad Bluetooth/USB ao cliente conforme seu uso habitual. Não há mudança nova no mapeamento ou no protocolo de controle. Imagem, áudio, vibração e reconexão ainda precisam ser conferidos no seu aparelho.

Para a primeira coleta, use **1080p, 60 FPS, H.264, HDR desligado**, com o mesmo bitrate e modo da TV em ambos os clientes. Isso é uma configuração inicial de diagnóstico, não uma configuração vencedora medida. Repita com HEVC se suportado e com 720p60 H.264 para investigar limitação de hardware. Preserve essas condições entre baseline e candidato.

## Gravar e exportar uma sessão

No APK debug, em **Malha Stream — Diagnóstico**, ative **Gravar diagnóstico da sessão**. Deixe **Marcadores de trace Android** desligado para a comparação inicial. Mantenha **Experimento HEVC MiTV-AYFR0** desligado: ele só se aplica ao modelo MiTV-AYFR0, API 34 e decoder exato `c2.amlogic.hevc.decoder`; não foi aprovado em teste físico Malha.

Inicie o streaming, execute a cena por pelo menos **120 segundos** e encerre normalmente pelo cliente. Espere a exportação terminar. **Exportar última sessão** compartilha JSON/CSV quando a TV dispõe de aplicativo de compartilhamento. A alternativa que dispensa esse aplicativo é, no CT:

```bash
python3 -m benchmark collect --serial IP_DA_TV:5555 --out benchmark/results/sessao-01
```

O diretório contém pares `<UUID>.json`/`<UUID>.csv`, relatórios `<UUID>.pipeline.json` e `collection.json`. O coletor copia todas as sessões disponíveis, sem apagar nenhuma. Identifique pelo UUID, horário, commit e configuração; diretórios diferentes não garantem sessões diferentes. No JSON, confira duração, perdas do buffer e campos indisponíveis antes de interpretar percentis.

Os tempos internos são medidas/estimativas do pipeline, com origem documentada. Não são input-to-photon e não confirmam FPS visível. O instante de liberar um buffer não prova que o pixel apareceu no painel.

## Preparar uma rodada reproduzível

O runner pode configurar o **Malha debug** via ADB. Não inicia jogo, não faz pairing e não altera clientes de terceiros:

```bash
python3 -m benchmark plan --device firetv --codec h264 --resolution 1080p --fps 60 --bitrate-kbps 20000 --profile upstream --out benchmark/results/plan.json
python3 -m benchmark probe --serial IP_DA_TV:5555 --out benchmark/results/device.json
python3 -m benchmark prepare --serial IP_DA_TV:5555 --plan benchmark/results/plan.json --out benchmark/results/prepared.json
```

Depois, abra manualmente a mesma cena, registre ≥120 s, encerre e execute `collect`. `--device firetv` é uma etiqueta do plano, não detecção do SoC. `upstream` e `lowest-latency` usam um modo de pacing que já existe no Moonlight; não são otimizações novas.

Compare Moonlight oficial, Artemis/V+ e Malha com condições equivalentes, três sessões por configuração e ordem intercalada. Para medir **input-to-photon**, filme uma referência observável do acionamento e a mudança da imagem, preferencialmente em ≥240 FPS, com ≥30 eventos válidos por sessão. Monitor do PC versus TV mede atraso relativo entre displays. O protocolo, importação de câmera e comparação estão em [PERFORMANCE.md](PERFORMANCE.md).

## Recompilar e testar

No CT, como `malha`, na raiz do repositório:

```bash
./gradlew --no-daemon --max-workers=2 :telemetry:check :app:assembleNonRootDebug :app:assembleNonRootRelease :app:assembleNonRootDebugAndroidTest :app:lintNonRootDebug
python3 -m unittest discover -s tests -v
```

O novo debug sai em `app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk`. A release Gradle sai **unsigned**; o comando acima não altera os APKs publicados em `artifacts`. No terminal do PVE, o operador pode assinar a nova build usando a chave protegida do CT:

```bash
pct exec 105 -- bash /home/malha/malha-stream/scripts/sign-release.sh
```

Esse passo atualiza os três APKs, `SHA256SUMS` e `SOURCE_COMMIT` no diretório local de artefatos. Não publica release no GitHub automaticamente. Preserve o commit e os logs para cada nova distribuição. A chave privada fica em `/root/.malha-signing` dentro do CT, fora do Git; seu backup cifrado fica no repositório privado de recuperação.

## Recuperar os containers antigos

Os CTs existentes no intervalo autorizado eram **205, 207, 208 e 212–218**. Foram desligados, preservados e removidos. Checkpoints do código e backups completos cifrados estão no [repositório privado pve-recovery](https://github.com/spy-exe/pve-recovery), com manifestos que distinguem as verificações realizadas. Foi validada a restauração do filesystem do CT 205 em um CT temporário desligado, depois removido; o boot dos aplicativos antigos não foi validado.

No PVE, como root:

```bash
/root/invocar-ct 205
pct config 205
```

O gatilho requer autenticação GitHub existente, manifestos locais e chave. Baixa e verifica o backup e restaura **desligado**, com `onboot=0`; recusa IDs ocupados. Revise capacidade de disco, IP e integrações e só então, se apropriado, execute `pct start 205`. Para outro ID livre: `/root/invocar-ct 205 --target-id ID_LIVRE`.

**Guarde `/root/pve-recovery/keys/archive.key` fora deste PVE, em local privado e seguro. Sem essa chave, os backups cifrados dos CTs e da assinatura não podem ser recuperados.** Ela não está no GitHub. Manifestos e relatório local ficam em `/root/pve-recovery`; instruções completas estão no README privado.
