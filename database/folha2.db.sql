BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "beneficios" (
	"id"	INTEGER,
	"nome"	TEXT NOT NULL UNIQUE,
	"descricao"	TEXT,
	"tipo"	TEXT NOT NULL,
	"valor_padrao"	REAL DEFAULT 0,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "cargos" (
	"id"	INTEGER,
	"nome"	TEXT NOT NULL UNIQUE,
	"descricao"	TEXT,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "empresas" (
	"id"	INTEGER,
	"razao_social"	TEXT NOT NULL,
	"nome_fantasia"	TEXT NOT NULL,
	"cnpj"	TEXT NOT NULL UNIQUE,
	"email"	TEXT NOT NULL,
	"telefone"	TEXT,
	"data_cadastro"	TEXT NOT NULL,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "folha_funcionarios" (
	"id"	INTEGER,
	"folha_id"	INTEGER NOT NULL,
	"funcionario_id"	INTEGER NOT NULL,
	"salario_base"	REAL NOT NULL,
	"total_proventos"	REAL NOT NULL,
	"salario_bruto"	REAL NOT NULL,
	"base_inss"	REAL NOT NULL,
	"valor_inss"	REAL NOT NULL,
	"base_fgts"	REAL NOT NULL,
	"valor_fgts"	REAL NOT NULL,
	"base_irpf"	REAL NOT NULL,
	"valor_irpf"	REAL NOT NULL,
	"total_descontos"	REAL NOT NULL,
	"salario_liquido"	REAL NOT NULL, 
	"status"	TEXT DEFAULT 'CONFIRMADA',
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("folha_id") REFERENCES "folhas_pagamento"("id"),
	FOREIGN KEY("funcionario_id") REFERENCES "funcionarios"("id")
);
CREATE TABLE IF NOT EXISTS "folhas_pagamento" (
	"id"	INTEGER,
	"mes"	INTEGER NOT NULL,
	"ano"	INTEGER NOT NULL,
	"data_pagamento"	TEXT,
	"status"	TEXT NOT NULL,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "funcionarios" (
	"id"	INTEGER,
	"nome"	TEXT NOT NULL,
	"cpf"	TEXT NOT NULL UNIQUE,
	"data_nascimento"	TEXT NOT NULL,
	"data_admissao"	TEXT NOT NULL,
	"cargo"	TEXT NOT NULL,
	"salario_base"	REAL NOT NULL,
	"empresa_id"	INTEGER,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "funcionarios_beneficios" (
	"id"	INTEGER,
	"funcionario_id"	INTEGER NOT NULL,
	"beneficio_id"	INTEGER NOT NULL,
	"valor"	REAL NOT NULL,
	"desconto_folha"	REAL NOT NULL DEFAULT 0,
	"data_inicio"	TEXT NOT NULL,
	"data_fim"	TEXT,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("beneficio_id") REFERENCES "beneficios"("id"),
	FOREIGN KEY("funcionario_id") REFERENCES "funcionarios"("id")
);
CREATE TABLE IF NOT EXISTS "lancamentos_folha" (
	"id"	INTEGER,
	"folha_funcionario_id"	INTEGER NOT NULL,
	"tipo_lancamento_id"	INTEGER NOT NULL,
	"referencia"	REAL,
	"valor"	REAL NOT NULL,
	"descricao"	TEXT,
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("folha_funcionario_id") REFERENCES "folha_funcionarios"("id"),
	FOREIGN KEY("tipo_lancamento_id") REFERENCES "tipos_lancamento"("id")
);
CREATE TABLE IF NOT EXISTS "tipos_lancamento" (
	"id"	INTEGER,
	"nome"	TEXT NOT NULL UNIQUE,
	"tipo"	TEXT NOT NULL,
	"descricao"	TEXT,
	"incide_inss"	INTEGER NOT NULL DEFAULT 0,
	"incide_fgts"	INTEGER NOT NULL DEFAULT 0,
	"incide_irpf"	INTEGER NOT NULL DEFAULT 0,
	"operacao_base"	TEXT NOT NULL DEFAULT 'SOMA',
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "usuarios" (
	"id"	INTEGER,
	"empresa_id"	INTEGER NOT NULL,
	"usuario"	TEXT NOT NULL UNIQUE,
	"senha"	TEXT NOT NULL,
	"email"	TEXT NOT NULL,
	"ativo"	INTEGER NOT NULL DEFAULT 1,
	"data_cadastro"	TEXT NOT NULL,
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("empresa_id") REFERENCES "empresas"("id")
);
INSERT INTO "beneficios" VALUES (1,'Vale Transporte','Benefício destinado ao deslocamento do funcionário','TRANSPORTE',300.0,1);
INSERT INTO "beneficios" VALUES (2,'Vale Refeição','Benefício destinado à alimentação durante a jornada de trabalho','ALIMENTACAO',500.0,1);
INSERT INTO "beneficios" VALUES (3,'Vale Alimentação','Benefício destinado à compra de alimentos','ALIMENTACAO',400.0,1);
INSERT INTO "beneficios" VALUES (4,'Plano de Saúde','Assistência médica oferecida ao funcionário','SAUDE',250.0,1);
INSERT INTO "beneficios" VALUES (5,'Plano Odontológico','Assistência odontológica oferecida ao funcionário','SAUDE',50.0,1);
INSERT INTO "cargos" VALUES (1,'Auxiliar Administrativo','Profissional responsável por atividades administrativas');
INSERT INTO "cargos" VALUES (2,'Analista Administrativo','Profissional responsável por atividades de análise e suporte administrativo');
INSERT INTO "empresas" VALUES (1,'Administracao de Facilities LTDA','Administracao de Facilities','12345678000199','contato@facilities.com.br','(31) 3333-4444','2026-10-03',1);
INSERT INTO "folha_funcionarios" VALUES (1,1,1,2500.0,2727.27,2727.27,2627.27,212.14,2627.27,210.18,2415.13,0.0,312.14,2415.13);
INSERT INTO "folhas_pagamento" VALUES (1,9,2026,'2026-09-30','ABERTA');
INSERT INTO "funcionarios" VALUES (1,'João da Silva','11111111111','1995-05-20','2026-01-10','Auxiliar Administrativo',2500.0,1);
INSERT INTO "funcionarios" VALUES (2,'Maria Oliveira','22222222222','1998-08-15','2026-02-05','Analista Administrativo',3500.0,1);
INSERT INTO "funcionarios_beneficios" VALUES (1,1,1,300.0,150.0,'2026-01-10',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (2,1,2,500.0,50.0,'2026-01-10',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (3,1,4,250.0,100.0,'2026-01-10',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (4,2,1,300.0,150.0,'2026-02-05',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (5,2,3,400.0,0.0,'2026-02-05',NULL,1);
INSERT INTO "funcionarios_beneficios" VALUES (6,2,5,50.0,25.0,'2026-02-05',NULL,1);
INSERT INTO "lancamentos_folha" VALUES (1,1,1,30.0,2500.0,'Salário mensal - Setembro/2026');
INSERT INTO "lancamentos_folha" VALUES (2,1,11,NULL,212.14,'INSS - Setembro/2026 - calculado');
INSERT INTO "lancamentos_folha" VALUES (3,1,2,10.0,227.27,'10 horas extras com adicional de 50%');
INSERT INTO "lancamentos_folha" VALUES (4,1,13,NULL,100.0,'Falta - Setembro/2026');
INSERT INTO "tipos_lancamento" VALUES (1,'Salário Base','PROVENTO','Salário mensal do funcionário',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (2,'Hora Extra 50%','PROVENTO','Hora extra com adicional de 50%',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (3,'Hora Extra 100%','PROVENTO','Hora extra com adicional de 100%',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (4,'Adicional Noturno','PROVENTO','Adicional referente ao trabalho noturno',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (5,'Insalubridade','PROVENTO','Adicional de insalubridade',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (6,'Periculosidade','PROVENTO','Adicional de periculosidade',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (7,'Comissão','PROVENTO','Comissões recebidas pelo funcionário',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (8,'DSR','PROVENTO','Descanso Semanal Remunerado',1,1,1,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (9,'Vale Transporte','DESCONTO','Desconto referente ao vale transporte',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (10,'Vale Refeição','DESCONTO','Desconto referente ao vale refeição',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (11,'INSS','DESCONTO','Contribuição previdenciária',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (12,'IRPF','DESCONTO','Imposto de Renda Retido na Fonte',0,0,0,'SOMA');
INSERT INTO "tipos_lancamento" VALUES (13,'Falta','DESCONTO','Desconto referente a faltas',1,1,1,'SUBTRAI');
INSERT INTO "tipos_lancamento" VALUES (14,'Atraso','DESCONTO','Desconto referente a atrasos',1,1,1,'SUBTRAI');
INSERT INTO "tipos_lancamento" VALUES (15,'Adiantamento Salarial','DESCONTO','Adiantamento recebido anteriormente',0,0,0,'SOMA');
INSERT INTO "usuarios" VALUES (1,1,'admin_facilities','123456','admin@facilities.com.br',1,'2026-10-03');
CREATE UNIQUE INDEX IF NOT EXISTS "idx_folha_funcionario_unico" ON "folha_funcionarios" (
	"folha_id",
	"funcionario_id"
);
CREATE UNIQUE INDEX IF NOT EXISTS "idx_funcionario_beneficio_ativo" ON "funcionarios_beneficios" (
	"funcionario_id",
	"beneficio_id",
	"ativo"
);
COMMIT;
