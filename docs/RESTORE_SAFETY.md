# Segurança da restauração

A restauração **substitui integralmente** os dados do aplicativo pelos dados do
arquivo escolhido. Não há mesclagem: projetos, disciplinas, assuntos, revisões e
preferências passam a ser exatamente os do backup. Por isso o processo é feito em
etapas recuperáveis, para que uma interrupção (bateria, encerramento pelo sistema,
travamento) nunca deixe o aplicativo em estado inconsistente.

## Fluxo

1. **Leitura e análise** — o aplicativo lê o arquivo escolhido pelo usuário e o
   valida por completo (ver `BACKUP_FORMAT.md`). Nada é gravado nesta etapa.
2. **Confirmação** — é exibida uma prévia com data de criação, versão de formato e
   contagens de projetos, disciplinas, assuntos e revisões. O usuário confirma
   explicitamente.
3. **Snapshot de segurança** — o estado atual é serializado e gravado em
   armazenamento privado do aplicativo, em `backup_seguranca/`, antes de qualquer
   alteração.
4. **Substituição do banco** — dentro de **uma única transação** Room: apaga tudo e
   reinsere os dados do backup, preservando os IDs. Em qualquer falha, a transação
   é revertida e os dados atuais permanecem intactos.
5. **Preferências** — grava um **marcador de operação** com as preferências do
   backup, aplica essas preferências e então remove o marcador.
6. **Conclusão** — apaga o snapshot de segurança e reagenda as notificações.

## Por que o marcador existe

Room (banco de dados) e DataStore (preferências) **não participam da mesma
transação**. Se o aplicativo for encerrado entre o passo 4 e o passo 5, o banco já
contém os dados do backup, mas as preferências ainda seriam as antigas. Para
cobrir exatamente essa janela:

- o marcador é gravado **depois** de o banco ser substituído e **antes** de as
  preferências serem aplicadas;
- na próxima abertura, o `ReconciliadorRestauracao` verifica o marcador, aplica as
  preferências pendentes, reagenda as notificações e o remove.

Assim, o resultado final é sempre "banco + preferências do backup", mesmo que a
aplicação seja interrompida no meio.

## Estados possíveis

| Situação | Estado final |
|---|---|
| Concluído normalmente | Dados e preferências do backup; snapshot apagado. |
| Falha ao substituir o banco | Nada muda (rollback da transação); snapshot apagado. |
| Interrupção após substituir o banco | Banco com dados do backup; marcador pendente; preferências concluídas na próxima abertura. |
| Cancelamento do usuário durante a restauração | Igual à interrupção: `CancellationException` é propagada, o banco mantém o que já foi confirmado e o marcador, se gravado, é reconciliado depois. |

## Snapshot de segurança

O snapshot é um arquivo `.revisaciclo` do estado imediatamente anterior à
restauração, mantido em `backup_seguranca/` (armazenamento privado do app).

- Em caso de sucesso, é apagado automaticamente ao final.
- Em caso de falha ao aplicar as preferências, é **mantido** para permitir
  recuperação manual por usuários avançados.

> O snapshot **não** substitui um backup exportado: mantenha sempre um arquivo
> `.revisaciclo` guardado fora do aplicativo.

## Backup automático do sistema (`allowBackup`)

O aplicativo desativa o backup automático do Android (`android:allowBackup="false"`)
e restringe a transferência entre dispositivos com `android:dataExtractionRules`
(Android 12+) e `android:fullBackupContent` (Android 11 e anteriores), excluindo
todos os domínios.

Motivo: Room (banco) e DataStore (preferências) **não são restaurados de forma
atômica** pelo sistema. O backup automático pode repor um dos dois lados sem o
outro — ou repor ambos sem executar a reconciliação descrita acima — deixando o
aplicativo em um estado inconsistente que o usuário não conseguiria diagnosticar.
Além disso, os dados de estudo não são enviados a nuvem alguma sem uma ação
explícita.

A forma suportada de backup é o próprio recurso do aplicativo: o usuário gera um
arquivo `.revisaciclo` e o guarda onde quiser (ver `BACKUP_FORMAT.md`). Assim a
restauração é validada por checksum e sempre passa pelo fluxo transacional seguro
descrito neste documento.

## O que a restauração não faz

- Não mescla dados; não há deduplicação.
- Não apaga o snapshot nem o backup exportado por você.
- Não usa permissões amplas de armazenamento: o acesso ao arquivo é concedido pelo
  usuário via Storage Access Framework (SAF).
- Não garante o horário exato das notificações (o Android pode atrasar por Doze).