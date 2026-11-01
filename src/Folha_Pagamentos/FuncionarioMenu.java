package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Scanner;

public class FuncionarioMenu {

	// Lista os funcionários da empresa e devolve o ID do escolhido (0 se não achar)
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

		// Confere se o ID digitado é de um funcionário dessa empresa
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

	// Altera o cargo ou o salário base de um funcionário
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
			// Altera o cargo
			String novoCargo = Leitor.lerObrigatorio(sc, "Novo cargo: ");
			PreparedStatement stmt = conn
					.prepareStatement("UPDATE funcionarios SET cargo = ? WHERE id = ? AND empresa_id = ?");
			stmt.setString(1, novoCargo);
			stmt.setInt(2, funcionarioId);
			stmt.setInt(3, empresaId);
			stmt.executeUpdate();
			System.out.println("Cargo atualizado com sucesso!");
		} else if (opcao == 2) {
			// Altera o salário base (não aceita valor negativo)
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

	// Cadastra um funcionário novo na empresa
	public static void cadastrarFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {

		funcionario funcionario = new funcionario();

		// Pede os dados pessoais
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

		// Pede os dados do trabalho
		System.out.print("Cargo: ");
		funcionario.Cargo = sc.nextLine();

		System.out.print("Salário Base: ");
		double salarioBase = sc.nextDouble();
		while (salarioBase < 0) {
			System.out.print("Salário não pode ser negativo. Digite novamente: ");
			salarioBase = sc.nextDouble();
		}

		// Salva o funcionário no banco
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
}
