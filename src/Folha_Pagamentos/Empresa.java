package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Scanner;

public class Empresa {

	// Mostra o menu inicial e só sai quando a empresa entrar no sistema
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

	// Login da empresa: pede CNPJ e senha, com até 3 tentativas
	public static int entrarEmpresa(Scanner sc, Connection conn) throws SQLException {
		String cnpj = Leitor.lerCnpj(sc);

		for (int tentativa = 1; tentativa <= 3; tentativa++) {
			String senha = Leitor.lerObrigatorio(sc, "Senha: ");

			// Busca a empresa e confere a senha digitada com a do banco
			try (PreparedStatement stmt = conn.prepareStatement("SELECT e.id, e.nome_fantasia, u.senha FROM empresas e "
					+ "JOIN usuarios u ON u.empresa_id = e.id WHERE e.cnpj = ? AND e.ativo = 1 AND u.ativo = 1")) {
				stmt.setString(1, cnpj);
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						if (Senha.confereSenha(senha, rs.getString("senha"))) {
							System.out.println("Bem-vindo, " + rs.getString("nome_fantasia") + "!");
							return rs.getInt("id");
						}
					}
				}
			}

			System.out.println("CNPJ ou senha incorretos. Tentativa " + tentativa + " de 3.");
		}

		System.out.println("Muitas tentativas. Voltando ao menu.");
		return 0;
	}

	// Redefine a senha depois de conferir CNPJ, email e telefone da empresa
	public static void redefinirSenha(Scanner sc, Connection conn) throws SQLException {
		System.out.println("Para redefinir a senha, confirme os dados da empresa.");
		String cnpj = Leitor.lerCnpj(sc);
		String email = Leitor.lerObrigatorio(sc, "Email da empresa: ");
		String telefone = Leitor.lerObrigatorio(sc, "Telefone da empresa: ");

		// Busca a empresa e confere se email e telefone batem com o cadastro
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

		// Pede a nova senha e salva o hash no banco
		System.out.println("Dados conferidos! Defina a nova senha.");
		String novaSenha = Leitor.lerSenhaComConfirmacao(sc);

		PreparedStatement stmtUp = conn.prepareStatement("UPDATE usuarios SET senha = ? WHERE empresa_id = ?");
		stmtUp.setString(1, Senha.criarHash(novaSenha));
		stmtUp.setInt(2, empresaId);
		int alterados = stmtUp.executeUpdate();

		// Se a empresa ainda não tinha usuário, cria um
		if (alterados == 0) {
			PreparedStatement stmtUser = conn.prepareStatement(
					"INSERT INTO usuarios (empresa_id, usuario, senha, email, data_cadastro) VALUES (?, ?, ?, ?, ?)");
			stmtUser.setInt(1, empresaId);
			stmtUser.setString(2, cnpj);
			stmtUser.setString(3, Senha.criarHash(novaSenha));
			stmtUser.setString(4, email);
			stmtUser.setString(5, LocalDate.now().toString());
			stmtUser.executeUpdate();
		}

		System.out.println("Senha redefinida com sucesso! Agora escolha Entrar.");
	}

	// Cadastra uma empresa nova com o usuário e a senha dela
	public static void cadastrarEmpresa(Scanner sc, Connection conn) throws SQLException {
		// Pede o CNPJ e confere se a empresa já existe
		String cnpj = Leitor.lerCnpj(sc);

		PreparedStatement stmtBusca = conn.prepareStatement("SELECT id FROM empresas WHERE cnpj = ?");
		stmtBusca.setString(1, cnpj);
		if (stmtBusca.executeQuery().next()) {
			System.out.println("Essa empresa já está cadastrada. Use a opção Entrar.");
			return;
		}

		// Pede os dados da empresa e a senha
		String razaoSocial = Leitor.lerObrigatorio(sc, "Razão social: ");
		String nomeFantasia = Leitor.lerObrigatorio(sc, "Nome fantasia: ");
		String email = Leitor.lerObrigatorio(sc, "Email: ");
		String telefone = Leitor.lerObrigatorio(sc, "Telefone: ");
		String senha = Leitor.lerSenhaComConfirmacao(sc);

		// Salva a empresa no banco
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

		// Cria o usuário da empresa com a senha em hash
		PreparedStatement stmtUser = conn.prepareStatement(
				"INSERT INTO usuarios (empresa_id, usuario, senha, email, data_cadastro) VALUES (?, ?, ?, ?, ?)");
		stmtUser.setInt(1, empresaId);
		stmtUser.setString(2, cnpj);
		stmtUser.setString(3, Senha.criarHash(senha));
		stmtUser.setString(4, email);
		stmtUser.setString(5, LocalDate.now().toString());
		stmtUser.executeUpdate();

		System.out.println("Empresa cadastrada com sucesso! Agora escolha Entrar.");
	}
}
