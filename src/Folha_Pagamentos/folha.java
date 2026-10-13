package Folha_Pagamentos;

import java.time.LocalDate;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class folha {

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		sc.useLocale(Locale.of("pt", "BR"));
		String url = "jdbc:sqlite:database/folha.db";

		try (Connection conn = DriverManager.getConnection(url)) {

			int empresaId = menuInicial(sc, conn);

			int opcao = -1;
			while (opcao != 0) {
				System.out.println();
				System.out.println("1 - Cadastrar funcionário");
				System.out.println("2 - Lançar folha de pagamento");
				System.out.println("3 - Histórico de folhas");
				System.out.println("4 - Benefícios");
				System.out.println("5 - Editar funcionário");
				System.out.println("6 - Folhas pendentes");
				System.out.println("0 - Sair");
				System.out.print("Escolha: ");
				opcao = sc.nextInt();
				sc.nextLine();

				if (opcao == 1) {
					cadastrarFuncionario(sc, conn, empresaId);
				} else if (opcao == 2) {
					lancarFolha(sc, conn, empresaId);
				} else if (opcao == 3) {
					historicoFolhas(conn, empresaId);
				} else if (opcao == 4) {
					menuBeneficios(sc, conn, empresaId);
				} else if (opcao == 5) {
					editarFuncionario(sc, conn, empresaId);
				} else if (opcao == 6) {
					folhasAConfirmar(sc, conn, empresaId);
				} else if (opcao != 0) {
					System.out.println("Opção inválida.");
				}
			}

		} catch (SQLException e) {
			System.out.println("Erro no banco: " + e.getMessage());
		}
	}

	// ===================== LER CAMPO OBRIGATORIO =====================
	public static String lerObrigatorio(Scanner sc, String texto) {
		System.out.print(texto);
		String valor = sc.nextLine().trim();
		while (valor.isEmpty()) {
			System.out.print("Campo obrigatório. " + texto);
			valor = sc.nextLine().trim();
		}

		return valor;
	}

	// ===================== MENU INICIAL =====================
	public static int menuInicial(Scanner sc, Connection conn) throws SQLException {
		int empresaId = 0;
		while (empresaId == 0) {
			System.out.println("1 - Entrar");
			System.out.println("2 - Cadastrar empresa");
			System.out.println("3 - Redefinir senha");
			System.out.print("Escolha: ");
			int opcao = sc.nextInt();
			sc.nextLine();

			if (opcao == 1) {
				empresaId = entrarEmpresa(sc, conn);
			} else if (opcao == 2) {
				cadastrarEmpresa(sc, conn);
			} else if (opcao == 3) {
				redefinirSenha(sc, conn);
			} else {
				System.out.println("Opção inválida.");
			}
		}
		return empresaId;
	}

	// ===================== LER CNPJ =====================
	public static String lerCnpj(Scanner sc) {
		String cnpj = lerObrigatorio(sc, "CNPJ da empresa (só números): ");
		while (!cnpj.matches("\\d{14}")) {
			System.out.print("CNPJ deve ter 14 números. Digite novamente: ");
			cnpj = sc.nextLine().trim();
		}
		return cnpj;
	}

	// ===================== SENHA (HASH COM SALT) =====================
	public static String hashSenha(String senha, byte[] salt) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			byte[] hash = md.digest(senha.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String criarHash(String senha) {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt) + ":" + hashSenha(senha, salt);
	}

	public static boolean confereSenha(String senha, String guardado) {
		String[] partes = guardado.split(":");
		if (partes.length != 2)
			return false;
		byte[] salt = Base64.getDecoder().decode(partes[0]);
		String hash = hashSenha(senha, salt);
		return MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8), partes[1].getBytes(StandardCharsets.UTF_8));
	}

	// ===================== ENTRAR (LOGIN DA EMPRESA) =====================
	public static int entrarEmpresa(Scanner sc, Connection conn) throws SQLException {
		String cnpj = lerCnpj(sc);

		for (int tentativa = 1; tentativa <= 3; tentativa++) {
			String senha = lerObrigatorio(sc, "Senha: ");

			PreparedStatement stmt = conn.prepareStatement("SELECT e.id, e.nome_fantasia, u.senha FROM empresas e "
					+ "JOIN usuarios u ON u.empresa_id = e.id " + "WHERE e.cnpj = ? AND e.ativo = 1 AND u.ativo = 1");
			stmt.setString(1, cnpj);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				if (confereSenha(senha, rs.getString("senha"))) {
					System.out.println("Bem-vindo, " + rs.getString("nome_fantasia") + "!");
					return rs.getInt("id");
				}
			}

			System.out.println("CNPJ ou senha incorretos. Tentativa " + tentativa + " de 3.");
		}

		System.out.println("Muitas tentativas. Voltando ao menu.");
		return 0;
	}

	// ===================== REDEFINIR SENHA =====================
	public static void redefinirSenha(Scanner sc, Connection conn) throws SQLException {
		System.out.println("Para redefinir a senha, confirme os dados da empresa.");
		String cnpj = lerCnpj(sc);
		String email = lerObrigatorio(sc, "Email da empresa: ");
		String telefone = lerObrigatorio(sc, "Telefone da empresa: ");

		PreparedStatement stmt = conn
				.prepareStatement("SELECT id, email, telefone FROM empresas WHERE cnpj = ? AND ativo = 1");
		stmt.setString(1, cnpj);
		ResultSet rs = stmt.executeQuery();

		int empresaId = 0;
		boolean confere = false;
		if (rs.next()) {
			empresaId = rs.getInt("id");
			String telBanco = rs.getString("telefone");
			telBanco = (telBanco == null) ? "" : telBanco.replaceAll("\\D", "");
			confere = rs.getString("email").equalsIgnoreCase(email) && !telBanco.isEmpty()
					&& telBanco.equals(telefone.replaceAll("\\D", ""));
		}

		if (!confere) {
			System.out.println("Os dados não conferem. Não foi possível redefinir a senha.");
			return;
		}

		System.out.println("Dados conferidos! Defina a nova senha.");
		String novaSenha = lerSenhaComConfirmacao(sc);

		PreparedStatement stmtUp = conn.prepareStatement("UPDATE usuarios SET senha = ? WHERE empresa_id = ?");
		stmtUp.setString(1, criarHash(novaSenha));
		stmtUp.setInt(2, empresaId);
		int alterados = stmtUp.executeUpdate();

		if (alterados == 0) {
			PreparedStatement stmtUser = conn.prepareStatement(
					"INSERT INTO usuarios (empresa_id, usuario, senha, email, data_cadastro) VALUES (?, ?, ?, ?, ?)");
			stmtUser.setInt(1, empresaId);
			stmtUser.setString(2, cnpj);
			stmtUser.setString(3, criarHash(novaSenha));
			stmtUser.setString(4, email);
			stmtUser.setString(5, LocalDate.now().toString());
			stmtUser.executeUpdate();
		}

		System.out.println("Senha redefinida com sucesso! Agora escolha Entrar.");
	}

	// ===================== SENHA COM CONFIRMACAO =====================
	public static String lerSenhaComConfirmacao(Scanner sc) {
		while (true) {
			String senha = lerObrigatorio(sc, "Senha (mínimo 6 caracteres): ");
			if (senha.length() < 6) {
				System.out.println("Senha muito curta.");
				continue;
			}
			String confirmacao = lerObrigatorio(sc, "Confirme a senha: ");
			if (senha.equals(confirmacao)) {
				return senha;
			}
			System.out.println("As senhas não conferem. Tente novamente.");
		}
	}

	// ===================== CADASTRAR EMPRESA =====================
	public static void cadastrarEmpresa(Scanner sc, Connection conn) throws SQLException {
		String cnpj = lerCnpj(sc);

		PreparedStatement stmtBusca = conn.prepareStatement("SELECT id FROM empresas WHERE cnpj = ?");
		stmtBusca.setString(1, cnpj);
		if (stmtBusca.executeQuery().next()) {
			System.out.println("Essa empresa já está cadastrada. Use a opção Entrar.");
			return;
		}

		String razaoSocial = lerObrigatorio(sc, "Razão social: ");
		String nomeFantasia = lerObrigatorio(sc, "Nome fantasia: ");
		String email = lerObrigatorio(sc, "Email: ");
		String telefone = lerObrigatorio(sc, "Telefone: ");

		String senha = lerSenhaComConfirmacao(sc);

		PreparedStatement stmtNova = conn.prepareStatement(
				"INSERT INTO empresas (razao_social, nome_fantasia, cnpj, email, telefone, data_cadastro) VALUES (?, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
		stmtNova.setString(1, razaoSocial);
		stmtNova.setString(2, nomeFantasia);
		stmtNova.setString(3, cnpj);
		stmtNova.setString(4, email);
		stmtNova.setString(5, telefone);
		stmtNova.setString(6, LocalDate.now().toString());
		stmtNova.executeUpdate();

		ResultSet chave = stmtNova.getGeneratedKeys();
		chave.next();
		int empresaId = chave.getInt(1);

		PreparedStatement stmtUser = conn.prepareStatement(
				"INSERT INTO usuarios (empresa_id, usuario, senha, email, data_cadastro) VALUES (?, ?, ?, ?, ?)");
		stmtUser.setInt(1, empresaId);
		stmtUser.setString(2, cnpj);
		stmtUser.setString(3, criarHash(senha));
		stmtUser.setString(4, email);
		stmtUser.setString(5, LocalDate.now().toString());
		stmtUser.executeUpdate();

		System.out.println("Empresa cadastrada com sucesso! Agora escolha Entrar.");
	}

	// ===================== MENU DE BENEFICIOS =====================
	public static void menuBeneficios(Scanner sc, Connection conn, int empresaId) throws SQLException {
		PreparedStatement stmtComb = conn.prepareStatement(
				"INSERT OR IGNORE INTO beneficios (nome, descricao, tipo, valor_padrao) VALUES (?, ?, ?, ?)");
		stmtComb.setString(1, "Vale Combustível");
		stmtComb.setString(2, "Benefício destinado ao abastecimento do veículo do funcionário");
		stmtComb.setString(3, "TRANSPORTE");
		stmtComb.setDouble(4, 200.0);
		stmtComb.executeUpdate();

		int opcao = -1;
		while (opcao != 0) {
			System.out.println();
			System.out.println("=========== BENEFÍCIOS ===========");
			System.out.println("1 - Dar benefício a um funcionário");
			System.out.println("2 - Ver benefícios de um funcionário");
			System.out.println("3 - Editar benefício de um funcionário");
			System.out.println("4 - Remover benefício de um funcionário");
			System.out.println("0 - Voltar");
			System.out.print("Escolha: ");
			opcao = sc.nextInt();
			sc.nextLine();

			if (opcao == 1) {
				atribuirBeneficio(sc, conn, empresaId);
			} else if (opcao == 2) {
				listarBeneficios(sc, conn, empresaId);
			} else if (opcao == 3) {
				editarBeneficio(sc, conn, empresaId);
			} else if (opcao == 4) {
				removerBeneficio(sc, conn, empresaId);
			} else if (opcao != 0) {
				System.out.println("Opção inválida.");
			}
		}
	}

	// ===================== ESCOLHER FUNCIONARIO =====================
	public static int escolherFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement("SELECT id, nome FROM funcionarios WHERE empresa_id = ?");
		stmt.setInt(1, empresaId);
		ResultSet rs = stmt.executeQuery();

		System.out.println("Funcionários:");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getInt("id") + " - " + rs.getString("nome"));
		}
		if (!achou) {
			System.out.println("Nenhum funcionário cadastrado.");
			return 0;
		}

		System.out.print("Digite o ID do funcionário: ");
		int id = sc.nextInt();
		sc.nextLine();

		PreparedStatement stmtOk = conn.prepareStatement("SELECT 1 FROM funcionarios WHERE id = ? AND empresa_id = ?");
		stmtOk.setInt(1, id);
		stmtOk.setInt(2, empresaId);
		if (!stmtOk.executeQuery().next()) {
			System.out.println("Funcionário não encontrado.");
			return 0;
		}
		return id;
	}

	// ===================== DAR BENEFICIO =====================
	public static void atribuirBeneficio(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		PreparedStatement stmtLista = conn
				.prepareStatement("SELECT id, nome, valor_padrao FROM beneficios WHERE ativo = 1 ORDER BY nome");
		ResultSet lista = stmtLista.executeQuery();
		System.out.println("Benefícios disponíveis:");
		while (lista.next()) {
			System.out.println(lista.getInt("id") + " - " + lista.getString("nome") + " (padrão: "
					+ moeda.format(lista.getDouble("valor_padrao")) + ")");
		}

		System.out.print("Digite o ID do benefício: ");
		int beneficioId = sc.nextInt();
		sc.nextLine();

		PreparedStatement stmtBen = conn.prepareStatement("SELECT nome FROM beneficios WHERE id = ? AND ativo = 1");
		stmtBen.setInt(1, beneficioId);
		ResultSet rsBen = stmtBen.executeQuery();
		if (!rsBen.next()) {
			System.out.println("Benefício não encontrado.");
			return;
		}
		String nomeBeneficio = rsBen.getString("nome");

		PreparedStatement stmtJa = conn.prepareStatement(
				"SELECT 1 FROM funcionarios_beneficios WHERE funcionario_id = ? AND beneficio_id = ? AND ativo = 1");
		stmtJa.setInt(1, funcionarioId);
		stmtJa.setInt(2, beneficioId);
		if (stmtJa.executeQuery().next()) {
			System.out.println("Esse funcionário já tem o benefício " + nomeBeneficio + ".");
			return;
		}

		System.out.print("Valor do benefício: ");
		double valor = sc.nextDouble();
		while (valor < 0) {
			System.out.print("Valor não pode ser negativo. Digite novamente: ");
			valor = sc.nextDouble();
		}

		System.out.print("Desconto na folha do funcionário (0 se a empresa paga tudo): ");
		double desconto = sc.nextDouble();
		while (desconto < 0) {
			System.out.print("Desconto não pode ser negativo. Digite novamente: ");
			desconto = sc.nextDouble();
		}
		sc.nextLine();

		PreparedStatement stmtNovo = conn.prepareStatement(
				"INSERT INTO funcionarios_beneficios (funcionario_id, beneficio_id, valor, desconto_folha, data_inicio) VALUES (?, ?, ?, ?, ?)");
		stmtNovo.setInt(1, funcionarioId);
		stmtNovo.setInt(2, beneficioId);
		stmtNovo.setDouble(3, valor);
		stmtNovo.setDouble(4, desconto);
		stmtNovo.setString(5, LocalDate.now().toString());
		stmtNovo.executeUpdate();

		System.out.println(nomeBeneficio + " adicionado com sucesso!");
	}

	// ===================== VER BENEFICIOS DO FUNCIONARIO =====================
	public static void listarBeneficios(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}

		PreparedStatement stmt = conn.prepareStatement("SELECT b.nome, fb.valor, fb.desconto_folha, fb.data_inicio "
				+ "FROM funcionarios_beneficios fb " + "JOIN beneficios b ON b.id = fb.beneficio_id "
				+ "WHERE fb.funcionario_id = ? AND fb.ativo = 1 " + "ORDER BY b.nome");
		stmt.setInt(1, funcionarioId);
		ResultSet rs = stmt.executeQuery();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		System.out.println();
		System.out.println("=========== BENEFÍCIOS DO FUNCIONÁRIO ===========");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getString("nome") + " | Valor: " + moeda.format(rs.getDouble("valor"))
					+ " | Desconto na folha: " + moeda.format(rs.getDouble("desconto_folha")) + " | Desde: "
					+ rs.getString("data_inicio"));
		}
		if (!achou) {
			System.out.println("Nenhum benefício cadastrado.");
		}
		System.out.println("=================================================");
	}

	
	// ===================== ESCOLHER BENEFICIO DO FUNCIONARIO =====================
	public static int escolherBeneficioDoFuncionario(Scanner sc, Connection conn, int funcionarioId)
			throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT fb.id, b.nome, fb.valor, fb.desconto_folha FROM funcionarios_beneficios fb "
						+ "JOIN beneficios b ON b.id = fb.beneficio_id "
						+ "WHERE fb.funcionario_id = ? AND fb.ativo = 1 ORDER BY b.nome");
		stmt.setInt(1, funcionarioId);
		ResultSet rs = stmt.executeQuery();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		System.out.println("Benefícios do funcionário:");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getInt("id") + " - " + rs.getString("nome") + " | Valor: "
					+ moeda.format(rs.getDouble("valor")) + " | Desconto: "
					+ moeda.format(rs.getDouble("desconto_folha")));
		}
		if (!achou) {
			System.out.println("Esse funcionário não tem benefícios.");
			return 0;
		}

		System.out.print("Digite o ID do benefício: ");
		int id = sc.nextInt();
		sc.nextLine();

		PreparedStatement stmtOk = conn.prepareStatement(
				"SELECT 1 FROM funcionarios_beneficios WHERE id = ? AND funcionario_id = ? AND ativo = 1");
		stmtOk.setInt(1, id);
		stmtOk.setInt(2, funcionarioId);
		if (!stmtOk.executeQuery().next()) {
			System.out.println("Benefício não encontrado.");
			return 0;
		}
		return id;
	}

	// ===================== DESCONTO SOBRE O VALOR DO BENEFICIO? =====================
	public static boolean sobreValor(String nomeBeneficio) {
		String nome = nomeBeneficio.toLowerCase();
		return nome.contains("refei") || nome.contains("aliment");
	}
	// ===================== LIMITE DE DESCONTO =====================
	public static double limiteDesconto(String nomeBeneficio, double valor, double salarioBase) {
		String nome = nomeBeneficio.toLowerCase();
		if (nome.contains("transporte")) {
			return Math.min(salarioBase * 0.06, valor);
		} else if (nome.contains("refei") || nome.contains("aliment")) {
			return valor * 0.20;
		}
		return salarioBase * 0.10;
	}

	// ===================== EDITAR BENEFICIO =====================
	public static void editarBeneficio(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}
		int fbId = escolherBeneficioDoFuncionario(sc, conn, funcionarioId);
		if (fbId == 0) {
			return;
		}

		PreparedStatement stmtDados = conn.prepareStatement(
				"SELECT b.nome, f.salario_base FROM funcionarios_beneficios fb "
						+ "JOIN beneficios b ON b.id = fb.beneficio_id "
						+ "JOIN funcionarios f ON f.id = fb.funcionario_id WHERE fb.id = ?");
		stmtDados.setInt(1, fbId);
		ResultSet dados = stmtDados.executeQuery();
		dados.next();
		String nome = dados.getString("nome");
		double salarioBase = dados.getDouble("salario_base");

		System.out.print("Novo valor do benefício (R$): ");
		double valor = sc.nextDouble();
		while (valor < 0) {
			System.out.print("Valor não pode ser negativo. Digite novamente: ");
			valor = sc.nextDouble();
		}

		double limite = limiteDesconto(nome, valor, salarioBase);
		double base = sobreValor(nome) ? valor : salarioBase;
		String sobre = sobreValor(nome) ? "do valor do benefício" : "do salário";
		double desconto = 0;
		if (limite > 0 && base > 0) {
			double maxPct = limite / base * 100;
			System.out.println(String.format("Desconto máximo permitido: %.2f%% %s (R$ %.2f)", maxPct, sobre, limite));
			System.out.print("Desconto em % " + sobre + " (0 se a empresa paga tudo): ");
			double pct = sc.nextDouble();
			while (pct < 0 || pct > maxPct + 0.0001) {
				System.out.print("Percentual inválido. Digite entre 0 e " + String.format("%.2f", maxPct) + ": ");
				pct = sc.nextDouble();
			}
			desconto = Math.round(base * pct / 100 * 100) / 100.0;
		} else {
			System.out.println("Esse benefício não permite desconto na folha.");
		}
		sc.nextLine();

		PreparedStatement stmt = conn.prepareStatement(
				"UPDATE funcionarios_beneficios SET valor = ?, desconto_folha = ? WHERE id = ? AND funcionario_id = ?");
		stmt.setDouble(1, valor);
		stmt.setDouble(2, desconto);
		stmt.setInt(3, fbId);
		stmt.setInt(4, funcionarioId);
		stmt.executeUpdate();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		System.out.println(nome + " atualizado! Desconto na folha: " + moeda.format(desconto));
	}
	// ===================== REMOVER BENEFICIO =====================
	public static void removerBeneficio(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}
		int fbId = escolherBeneficioDoFuncionario(sc, conn, funcionarioId);
		if (fbId == 0) {
			return;
		}

		String resposta = lerObrigatorio(sc, "Tem certeza que quer remover? (s/n): ");
		if (resposta.equalsIgnoreCase("s")) {
			PreparedStatement stmt = conn.prepareStatement(
					"UPDATE funcionarios_beneficios SET ativo = 0 WHERE id = ? AND funcionario_id = ?");
			stmt.setInt(1, fbId);
			stmt.setInt(2, funcionarioId);
			stmt.executeUpdate();
			System.out.println("Benefício removido com sucesso!");
		} else {
			System.out.println("Nada foi removido.");
		}
	}
	// ===================== HISTORICO DE FOLHAS =====================
	public static void historicoFolhas(Connection conn, int empresaId) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT e.nome_fantasia, e.cnpj, fp.mes, fp.ano, fu.nome, fu.cargo, ff.salario_base, ff.total_proventos, ff.valor_inss, ff.valor_irpf, ff.total_descontos, ff.salario_liquido, ff.valor_fgts, ff.status "
						+ "FROM folha_funcionarios ff " + "JOIN folhas_pagamento fp ON fp.id = ff.folha_id "
						+ "JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "JOIN empresas e ON e.id = fu.empresa_id " + "WHERE fu.empresa_id = ? "
						+ "ORDER BY fp.ano DESC, fp.mes DESC, fu.nome");
		stmt.setInt(1, empresaId);
		ResultSet rs = stmt.executeQuery();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		boolean achou = false;
		while (rs.next()) {
			achou = true;
			String cnpj = rs.getString("cnpj").replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
			double beneficios = rs.getDouble("total_descontos") - rs.getDouble("valor_inss")
					- rs.getDouble("valor_irpf");

			System.out.println();
			System.out.println("=========== FOLHA DE PAGAMENTO ===========");
			System.out.println("Empresa: " + rs.getString("nome_fantasia"));
			System.out.println("CNPJ: " + cnpj);
			System.out.println("Referência: " + String.format("%02d/%d", rs.getInt("mes"), rs.getInt("ano")));
			System.out.println("Status: " + rs.getString("status"));
			System.out.println("Funcionário: " + rs.getString("nome"));
			System.out.println("Cargo: " + rs.getString("cargo"));
			System.out.println("------------------------------------------");
			System.out.println("PROVENTOS");
			System.out.println("Salário Base:    " + moeda.format(rs.getDouble("salario_base")));
			System.out.println("Total Proventos: " + moeda.format(rs.getDouble("total_proventos")));
			System.out.println("------------------------------------------");
			System.out.println("DESCONTOS");
			System.out.println("INSS:            " + moeda.format(rs.getDouble("valor_inss")));
			System.out.println("IRRF:            " + moeda.format(rs.getDouble("valor_irpf")));
			if (beneficios > 0.005) {
				System.out.println("Benefícios:      " + moeda.format(beneficios));
			}
			System.out.println("Total descontos: " + moeda.format(rs.getDouble("total_descontos")));
			System.out.println("------------------------------------------");
			System.out.println("SALÁRIO LÍQUIDO: " + moeda.format(rs.getDouble("salario_liquido")));
			System.out.println("------------------------------------------");
			System.out.println("FGTS (empresa):  " + moeda.format(rs.getDouble("valor_fgts")));
			System.out.println("==========================================");
		}
		if (!achou) {
			System.out.println("Nenhuma folha lançada ainda.");
		}
	}

	// ===================== FOLHAS A CONFIRMAR =====================
	public static void folhasAConfirmar(Scanner sc, Connection conn, int empresaId) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT ff.id, fp.mes, fp.ano, fu.nome, ff.salario_liquido FROM folha_funcionarios ff "
						+ "JOIN folhas_pagamento fp ON fp.id = ff.folha_id "
						+ "JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE fu.empresa_id = ? AND ff.status = 'PENDENTE' ORDER BY fp.ano, fp.mes, fu.nome");
		stmt.setInt(1, empresaId);
		ResultSet rs = stmt.executeQuery();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		System.out.println();
		System.out.println("=========== FOLHAS A CONFIRMAR ===========");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getInt("id") + " - " + String.format("%02d/%d", rs.getInt("mes"), rs.getInt("ano"))
					+ " - " + rs.getString("nome") + " | Líquido: " + moeda.format(rs.getDouble("salario_liquido")));
		}
		if (!achou) {
			System.out.println("Nenhuma folha pendente.");
			return;
		}

		System.out.print("Digite o ID da folha (0 para voltar): ");
		int id = sc.nextInt();
		sc.nextLine();
		if (id == 0) {
			return;
		}

		PreparedStatement stmtOk = conn.prepareStatement(
				"SELECT 1 FROM folha_funcionarios ff JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE ff.id = ? AND ff.status = 'PENDENTE' AND fu.empresa_id = ?");
		stmtOk.setInt(1, id);
		stmtOk.setInt(2, empresaId);
		if (!stmtOk.executeQuery().next()) {
			System.out.println("Folha pendente não encontrada.");
			return;
		}

		System.out.println("1 - Confirmar folha");
		System.out.println("2 - Apagar folha");
		System.out.println("0 - Voltar");
		System.out.print("Escolha: ");
		int opcao = sc.nextInt();
		sc.nextLine();

		if (opcao == 1) {
			PreparedStatement stmtConf = conn.prepareStatement(
					"UPDATE folha_funcionarios SET status = 'CONFIRMADA' WHERE id = ? AND status = 'PENDENTE'");
			stmtConf.setInt(1, id);
			stmtConf.executeUpdate();
			System.out.println("Folha confirmada com sucesso!");
		} else if (opcao == 2) {
			String resposta = lerObrigatorio(sc, "Tem certeza que quer apagar? (s/n): ");
			if (resposta.equalsIgnoreCase("s")) {
				PreparedStatement stmtApagar = conn
						.prepareStatement("DELETE FROM folha_funcionarios WHERE id = ? AND status = 'PENDENTE'");
				stmtApagar.setInt(1, id);
				stmtApagar.executeUpdate();
				System.out.println("Folha apagada com sucesso!");
			} else {
				System.out.println("Nada foi apagado.");
			}
		} else if (opcao != 0) {
			System.out.println("Opção inválida.");
		}
	}

	// ===================== EDITAR FUNCIONARIO =====================
	public static void editarFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}

		System.out.println();
		System.out.println("1 - Alterar cargo");
		System.out.println("2 - Alterar salário base");
		System.out.println("0 - Voltar");
		System.out.print("Escolha: ");
		int opcao = sc.nextInt();
		sc.nextLine();

		if (opcao == 1) {
			String novoCargo = lerObrigatorio(sc, "Novo cargo: ");
			PreparedStatement stmt = conn
					.prepareStatement("UPDATE funcionarios SET cargo = ? WHERE id = ? AND empresa_id = ?");
			stmt.setString(1, novoCargo);
			stmt.setInt(2, funcionarioId);
			stmt.setInt(3, empresaId);
			stmt.executeUpdate();
			System.out.println("Cargo atualizado com sucesso!");
		} else if (opcao == 2) {
			System.out.print("Novo salário base: ");
			double novoSalario = sc.nextDouble();
			while (novoSalario < 0) {
				System.out.print("Salário não pode ser negativo. Digite novamente: ");
				novoSalario = sc.nextDouble();
			}
			sc.nextLine();
			PreparedStatement stmt = conn
					.prepareStatement("UPDATE funcionarios SET salario_base = ? WHERE id = ? AND empresa_id = ?");
			stmt.setDouble(1, novoSalario);
			stmt.setInt(2, funcionarioId);
			stmt.setInt(3, empresaId);
			stmt.executeUpdate();
			System.out.println("Salário atualizado com sucesso!");
		} else if (opcao != 0) {
			System.out.println("Opção inválida.");
		}
	}

	// ===================== CADASTRAR FUNCIONÁRIO =====================
	public static void cadastrarFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {

		funcionario funcionario = new funcionario();

		System.out.print("Nome: ");
		funcionario.Nome = sc.nextLine();

		System.out.print("CPF: ");
		funcionario.Cpf = sc.nextLine();

		System.out.print("Data de nascimento (ddmmaaaa): ");
		String textoNascimento = sc.nextLine();
		funcionario.DataNascimento = LocalDate.of(Integer.parseInt(textoNascimento.substring(4, 8)),
				Integer.parseInt(textoNascimento.substring(2, 4)), Integer.parseInt(textoNascimento.substring(0, 2)));

		System.out.print("Data de admissao (ddmmaaaa): ");
		String textoAdmissao = sc.nextLine();
		funcionario.DataAdmissao = LocalDate.of(Integer.parseInt(textoAdmissao.substring(4, 8)),
				Integer.parseInt(textoAdmissao.substring(2, 4)), Integer.parseInt(textoAdmissao.substring(0, 2)));

		System.out.print("Cargo: ");
		funcionario.Cargo = sc.nextLine();

		System.out.print("Salário Base: ");
		double salarioBase = sc.nextDouble();
		while (salarioBase < 0) {
			System.out.print("Salário não pode ser negativo. Digite novamente: ");
			salarioBase = sc.nextDouble();
		}

		String sql = "INSERT INTO funcionarios (nome, cpf, data_nascimento, data_admissao, cargo, salario_base, empresa_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setString(1, funcionario.Nome);
		stmt.setString(2, funcionario.Cpf);
		stmt.setString(3, funcionario.DataNascimento.toString());
		stmt.setString(4, funcionario.DataAdmissao.toString());
		stmt.setString(5, funcionario.Cargo);
		stmt.setDouble(6, salarioBase);
		stmt.setInt(7, empresaId);
		stmt.executeUpdate();

		System.out.println("Funcionário cadastrado com sucesso!");
	}

	// ===================== LANÇAR FOLHA =====================
	public static void lancarFolha(Scanner sc, Connection conn, int empresaId) throws SQLException {
		// ===================== LISTAR FUNCIONARIOS =====================
		PreparedStatement stmtListar = conn
				.prepareStatement("SELECT id, nome, cargo FROM funcionarios WHERE empresa_id = ?");
		stmtListar.setInt(1, empresaId);
		ResultSet lista = stmtListar.executeQuery();

		System.out.println("Funcionários cadastrados:");
		while (lista.next()) {
			System.out.println(lista.getInt("id") + " - " + lista.getString("nome") + " - " + lista.getString("cargo"));
		}

		System.out.print("Digite o ID do funcionário: ");
		int idEscolhido = sc.nextInt();

		// ===================== BUSCAR SALARIO DELE =====================
		PreparedStatement stmtBuscar = conn
				.prepareStatement("SELECT nome, cargo, salario_base FROM funcionarios WHERE id = ? AND empresa_id = ?");
		stmtBuscar.setInt(1, idEscolhido);
		stmtBuscar.setInt(2, empresaId);
		ResultSet resultado = stmtBuscar.executeQuery();

		if (!resultado.next()) {
			System.out.println("Funcionário não encontrado.");
			return;
		}

		proventos proventos = new proventos();
		descontos descontos = new descontos();

		proventos.SalarioBase = resultado.getDouble("salario_base");
		proventos.TotalProventos = proventos.SalarioBase;
		String nomeFuncionario = resultado.getString("nome");
		String cargoFuncionario = resultado.getString("cargo");

		// ===================== BUSCAR EMPRESA =====================
		PreparedStatement stmtEmp = conn.prepareStatement("SELECT nome_fantasia, cnpj FROM empresas WHERE id = ?");
		stmtEmp.setInt(1, empresaId);
		ResultSet rsEmp = stmtEmp.executeQuery();
		rsEmp.next();
		String nomeEmpresa = rsEmp.getString("nome_fantasia");
		String cnpjEmpresa = rsEmp.getString("cnpj").replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})",
				"$1.$2.$3/$4-$5");

		// ===================== CALCULAR DESCONTOS =====================
		descontos.InssDesconto(proventos.TotalProventos);
		descontos.IrrfDesconto(proventos.TotalProventos, descontos.Inss);
		descontos.FgtsDesconto(proventos.TotalProventos);
		descontos.calcularTotalDescontos();

		double totalBeneficios = 0;
		StringBuilder linhasBeneficios = new StringBuilder();
		NumberFormat moedaBen = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		PreparedStatement stmtBen = conn.prepareStatement(
				"SELECT b.nome, fb.desconto_folha FROM funcionarios_beneficios fb JOIN beneficios b ON b.id = fb.beneficio_id WHERE fb.funcionario_id = ? AND fb.ativo = 1 AND fb.desconto_folha > 0");
		stmtBen.setInt(1, idEscolhido);
		ResultSet rsBen = stmtBen.executeQuery();
		while (rsBen.next()) {
			double desc = rsBen.getDouble("desconto_folha");
			totalBeneficios += desc;
			linhasBeneficios.append(rsBen.getString("nome") + ": " + moedaBen.format(desc) + "\n");
		}
		double totalDescontos = descontos.TotalDescontos + totalBeneficios;
		double salarioLiquido = proventos.TotalProventos - totalDescontos;

		// ===================== CRIAR A FOLHA DO MES =====================
		// =====================SE JA EXISTE FOLHA DO MES/ANO, USA ELA SE NAO, CRIAR UMA
		// NOVA=====================
		LocalDate hoje = LocalDate.now();
		int mes = hoje.getMonthValue();
		int ano = hoje.getYear();
		String dataPagamento = hoje.withDayOfMonth(hoje.lengthOfMonth()).toString();

		int folhaId;
		PreparedStatement stmtBusca = conn
				.prepareStatement("SELECT id FROM folhas_pagamento WHERE mes = ? AND ano = ?");
		stmtBusca.setInt(1, mes);
		stmtBusca.setInt(2, ano);
		ResultSet rsFolha = stmtBusca.executeQuery();

		if (rsFolha.next()) {
			folhaId = rsFolha.getInt("id");
		} else {
			PreparedStatement stmtFolha = conn.prepareStatement(
					"INSERT INTO folhas_pagamento (mes, ano, data_pagamento, status) VALUES (?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			stmtFolha.setInt(1, mes);
			stmtFolha.setInt(2, ano);
			stmtFolha.setString(3, dataPagamento);
			stmtFolha.setString(4, "ABERTA");
			stmtFolha.executeUpdate();
			ResultSet chave = stmtFolha.getGeneratedKeys();
			chave.next();
			folhaId = chave.getInt(1);
		}

		PreparedStatement stmtJa = conn
				.prepareStatement("SELECT 1 FROM folha_funcionarios WHERE folha_id = ? AND funcionario_id = ?");
		stmtJa.setInt(1, folhaId);
		stmtJa.setInt(2, idEscolhido);
		if (stmtJa.executeQuery().next()) {
			System.out.println("Esse funcionário já foi lançado neste mês. Veja a folha dele na opção 3 (Histórico).");
			return;
		}

		// ===================== INSERIR NA folha_funcionarios =====================
		PreparedStatement stmtFolhaFunc = conn.prepareStatement(
				"INSERT INTO folha_funcionarios (folha_id, funcionario_id, salario_base, total_proventos, salario_bruto, base_inss, valor_inss, base_fgts, valor_fgts, base_irpf, valor_irpf, total_descontos, salario_liquido, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDENTE')");
		stmtFolhaFunc.setInt(1, folhaId);
		stmtFolhaFunc.setInt(2, idEscolhido);
		stmtFolhaFunc.setDouble(3, proventos.SalarioBase);
		stmtFolhaFunc.setDouble(4, proventos.TotalProventos);
		stmtFolhaFunc.setDouble(5, proventos.TotalProventos);
		stmtFolhaFunc.setDouble(6, proventos.TotalProventos);
		stmtFolhaFunc.setDouble(7, descontos.Inss);
		stmtFolhaFunc.setDouble(8, proventos.TotalProventos);
		stmtFolhaFunc.setDouble(9, descontos.Fgts);
		stmtFolhaFunc.setDouble(10, proventos.TotalProventos - descontos.Inss);
		stmtFolhaFunc.setDouble(11, descontos.Irrf);
		stmtFolhaFunc.setDouble(12, totalDescontos);
		stmtFolhaFunc.setDouble(13, salarioLiquido);
		stmtFolhaFunc.executeUpdate();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		System.out.println();
		System.out.println("=========== FOLHA DE PAGAMENTO ===========");
		System.out.println("Empresa: " + nomeEmpresa);
		System.out.println("CNPJ: " + cnpjEmpresa);
		System.out.println("Referência: " + String.format("%02d/%d", mes, ano));
		System.out.println("Funcionário: " + nomeFuncionario);
		System.out.println("Cargo: " + cargoFuncionario);
		System.out.println("------------------------------------------");
		System.out.println("PROVENTOS");
		System.out.println("Salário Base:    " + moeda.format(proventos.SalarioBase));
		System.out.println("Total Proventos: " + moeda.format(proventos.TotalProventos));
		System.out.println("------------------------------------------");
		System.out.println("DESCONTOS");
		System.out.println("INSS:            " + moeda.format(descontos.Inss));
		System.out.println("IRRF:            " + moeda.format(descontos.Irrf));
		System.out.print(linhasBeneficios);
		System.out.println("Total descontos: " + moeda.format(totalDescontos));
		System.out.println("------------------------------------------");
		System.out.println("SALÁRIO LÍQUIDO: " + moeda.format(salarioLiquido));
		System.out.println("------------------------------------------");
		System.out.println("FGTS (empresa):  " + moeda.format(descontos.Fgts));
		System.out.println("==========================================");
	}
}