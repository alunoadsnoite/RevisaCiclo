# Conferência antes da formatação — 15/10/2026

Consultadas as branches atuais no GitHub de todos os repositórios de aplicativos encontrados em ~/Projetos, incluindo edições e cópias de trabalho. Todos os commits das branches locais estão alcançáveis nas branches do GitHub. Isso não inclui mudanças não commitadas, arquivos ignorados ou arquivos fora de repositórios.

## Pendências locais

- tea-mvp-childs: fdroid/metadata/com.teakids.app.yml e fdroid/reports/validation.md modificados.
- tea-mvp: F_DROID_RELEASE.md e app.json modificados; três novas screenshots (5-respiracao.png, 6-cartao-crise.png, 7-configuracoes.png) e plugins/withReleaseOptimization.js sem commit.
- bibliaappipm: docs/FDROID_PIPELINES_2026-10-15.md sem commit.
- bibliaappipm_gh: mudanças em LICENSE, README, código e documentação, exclusões de assets e docs/COMPLIANCE_2026-10-14.md sem commit. Preservar esta cópia antes de decidir se as mudanças ainda são necessárias.
- php/primeiro-projeto/index.php: fora de um repositório Git; sem backup GitHub verificado.

## Repositórios sem alterações próprias pendentes

FocusFeed, PixelRicardo, RevisaCiclo, Bíblia igreja (edicoes/igreja), Bíblia Livre Android (edicoes/biblia-livre-ipm) e Bíblia Livre Linux.
A pasta biblia-livre-linux aparece como não rastreada no RevisaCiclo, mas é um repositório independente, já enviado ao GitHub (codex/linux-appimage).
As 11 alterações de commits antes indicadas como ahead em tea-mvp já estão em branches do GitHub; não foram encontrados commits exclusivos locais após atualizar as referências.

## Arquivos que exigem backup privado

- ~/Projetos/FocusFeed/.private/focusfeed-release.jks
- ~/Projetos/FocusFeed/.private/key.properties
- ~/Projetos/bibliaappipm/flutter/android/release-keystore.jks
- ~/Projetos/bibliaappipm/flutter/android/key.properties

Não publicar chaves de assinatura ou senhas em repositórios públicos. Guardar cópia privada fora do disco que será formatado, com as senhas necessárias para uso das chaves.
Chaves debug.keystore também existem nos projetos TEA, mas não substituem as chaves de produção.

## Limites

Esta conferência não cria backup externo nem envia mudanças. O relatório está no mesmo disco e também será perdido com a formatação se não for copiado. Arquivos pessoais, configurações, credenciais e histórico de ferramentas precisam de backup separado conforme necessário. Não foi declarada segurança para formatar.
