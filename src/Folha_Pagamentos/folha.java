package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Scanner;

public class folha {

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		sc.useLocale(Locale.of("pt", "BR"));
		String url = "jdbc:sqlite:database/folha.db";

		try (Connection conn = DriverManager.getConnection(url)) {

			// Login ou cadastro da empresa
			int empresaId = Empresa.menuInicial(sc, conn);

			// Menu principal do sistema
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
					FuncionarioMenu.cadastrarFuncionario(sc, conn, empresaId);
				} else if (opcao == 2) {
					FolhaPagamento.lancarFolha(sc, conn, empresaId);
				} else if (opcao == 3) {
					Historico.menuHistorico(sc, conn, empresaId);
				} else if (opcao == 4) {
					Beneficio.menuBeneficios(sc, conn, empresaId);
				} else if (opcao == 5) {
					FuncionarioMenu.editarFuncionario(sc, conn, empresaId);
				} else if (opcao == 6) {
					FolhaPagamento.folhasAConfirmar(sc, conn, empresaId);
				} else if (opcao != 0) {
					System.out.println("Opção inválida.");
				}
			}

		} catch (SQLException e) {
			System.out.println("Erro no banco: " + e.getMessage());
		}
	}
}