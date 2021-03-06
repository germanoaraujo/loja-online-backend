package com.redhat.loja;

import static org.hamcrest.MatcherAssert.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.KieServices;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.redhat.loja.kie.model.ExecutionCommand;
import com.redhat.loja.kie.model.FireAllRulesCommand;
import com.redhat.loja.kie.model.InsertCommand;
import com.redhat.loja.kie.model.KieBatchExecution;
import com.redhat.loja.kie.model.KieObject;
import com.redhat.loja.kie.model.StartProcessCommand;
import com.redhat.loja_online.Cliente;
import com.redhat.loja_online.Compra;
import com.redhat.loja_online.Endereco;
import com.redhat.loja_online.Produto;
import com.redhat.loja_online.ProdutoCompra;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class KieRestTest {

	protected Client client = ClientBuilder.newClient();
	private static final String KIE_SESSION_NAME = "kieless";
	private static final String CONTAINER_ID = "loja-online_1.0.0-SNAPSHOT";

	protected WebTarget target = null;

	@Before
	public void createWebTarget() {
		target = client.target(
				"http://localhost:8080/kie-server/services/rest/server/containers/instances/loja-online_1.0.0-SNAPSHOT");
	}

	@Test
	@Ignore
	/**
	 * Demonstra como invocar o Decision Manager passando um payload de XML
	 */
	public void testCallXML() {

		StringBuilder sb = new StringBuilder("<batch-execution lookup=\"kieless\">");
		sb.append("<insert entry-point=\"DEFAULT\" out-identifier=\"Compra\" return-object=\"true\">"
				+ "<com.redhat.loja_online.Compra><valorTotal>100.0</valorTotal>" + "<cliente>" + "<idade>39</idade>"
				+ "<nivel>1</nivel>" + "<endereco>" + "<cep>52920062</cep>" + "</endereco>" + "</cliente>"
				+ "</com.redhat.loja_online.Compra>" + "</insert>" + "<start-process processId=\"totalizar\"/>"
				+ "<fire-all-rules/>" + "</batch-execution>");
		Builder request = target.request(MediaType.APPLICATION_XML_TYPE);
		request.header("X-KIE-ContentType", "XSTREAM");
		request.header("Authorization", "Basic cmhkbUFkbWluOnIzZGg0dDEh");

		String post = request.post(Entity.entity(sb.toString(), MediaType.APPLICATION_XML_TYPE), String.class);
		System.out.println(post);

	}

	@Test
	@Ignore
	/**
	 * Demonstra como invocar o Decision Manager sem utilizar a API gerando todo o JSON manualmente
	 */
	public void testCallManualJSON() {
		List<ProdutoCompra> prods = new ArrayList<>();
		prods.add(ProdutoCompra.builder()
				.produto(Produto.builder().codigo("123").descricao("abc").valorUnitario(10.0).build()).quantidade(8).valorTotal(80.0)
				.build());
		prods.add(ProdutoCompra.builder()
				.produto(Produto.builder().codigo("321").descricao("xyz").valorUnitario(10.0).build()).quantidade(5).valorTotal(50.0)
				.build());
		Compra c = Compra.builder().cliente(
				Cliente.builder().id(0L).idade(39).endereco(Endereco.builder().cep(54931470).build()).nivel(1).build())
				.valorTotal(100D).produtos(prods).build();

		String json = mountJson(c, "totalizar");

		Builder request = target.request(MediaType.APPLICATION_JSON_TYPE);
		request.header("X-KIE-ContentType", "JSON");
		request.header("Authorization", "Basic cmhkbUFkbWluOnIzZGg0dDEh");

		String post = request.post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE), String.class);

		Compra compra = extractResult(post);

		System.out.println(compra);

	}

	@Test
	@Ignore
	/**
	 * Demonstra como invocar o Decision Manager sem utilizar a API gerando JSON através de classes pojo.
	 */
	public void testCallGeneratedJSON() {
		List<ExecutionCommand> commands = new ArrayList<>();
		List<ProdutoCompra> prods = new ArrayList<>();
		prods.add(ProdutoCompra.builder()
				.produto(Produto.builder().codigo("123").descricao("abc").valorUnitario(10.0).build()).quantidade(8)
				.valorTotal(80.0).build());
		prods.add(ProdutoCompra.builder()
				.produto(Produto.builder().codigo("321").descricao("xyz").valorUnitario(10.0).build()).quantidade(5)
				.valorTotal(50.0).build());
		commands.add(InsertCommand.builder().entryPoint("DEFAULT").outIdentifier("Compra").returnObject(true)
				.object(KieObject.builder().objectName("com.redhat.loja_online.Compra")
						.compra(Compra.builder().valorTotal(0.0).produtos(prods)
								.cliente(Cliente.builder().nivel(1).id(0L).idade(39)
										.endereco(Endereco.builder().cep(50931470).build()).build())
								.build())
						.build())
				.build());
		commands.add(StartProcessCommand.builder().processId("totalizar").build());
		commands.add(FireAllRulesCommand.builder().build());

		KieBatchExecution execution = KieBatchExecution.builder().lookup("kieless").commands(commands).build();

		Builder request = target.request(MediaType.APPLICATION_JSON_TYPE);
		request.header("X-KIE-ContentType", "JSON");
		request.header("Authorization", "Basic cmhkbUFkbWluOnIzZGg0dDEh");

		String json = execution.toJson();

		System.out.println(json);
		
		String post = request.post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE), String.class);

		Compra compra = extractResult(post);

		System.out.println(compra);
	}

	@Test
	@Ignore
	/**
	 * Demonstra como invocar o Decision Manager através da API Java. 
	 * 
	 */
	public void testCallJavaAPI() {
		try {
			KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(
					"http://localhost:8080/kie-server/services/rest/server", "rhdmAdmin", "r3dh4t1!");
			conf.setMarshallingFormat(MarshallingFormat.JSON);
			KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);

			RuleServicesClient rulesClient = kieServicesClient.getServicesClient(RuleServicesClient.class);

			Compra compra = new Compra();
			Cliente cliente = new Cliente();
			Endereco endereco = new Endereco();
			endereco.setCep(50931470);
			cliente.setEndereco(endereco);
			cliente.setNivel(1);
			cliente.setIdade(39);
			compra.setCliente(cliente);
			compra.setValorTotal(100.0);

			List<ProdutoCompra> prods = new ArrayList<>();
			prods.add(ProdutoCompra.builder()
					.produto(Produto.builder().codigo("123").descricao("abc").valorUnitario(10.0).build()).quantidade(8).valorTotal(80.0)
					.build());
			prods.add(ProdutoCompra.builder()
					.produto(Produto.builder().codigo("321").descricao("xyz").valorUnitario(10.0).build()).quantidade(5).valorTotal(50.0)
					.build());
			compra.setProdutos(prods);

			KieCommands commandsFactory = KieServices.Factory.get().getCommands();
			List<Command<?>> commands = new ArrayList<>();

			commands.add(commandsFactory.newInsert(compra, "Compra", true, "DEFAULT"));
			commands.add(commandsFactory.newStartProcess("totalizar"));
			commands.add(commandsFactory.newFireAllRules());

			BatchExecutionCommand batchCommand = commandsFactory.newBatchExecution(commands, KIE_SESSION_NAME);
			ServiceResponse<ExecutionResults> executeResponse = rulesClient.executeCommandsWithResults(CONTAINER_ID,
					batchCommand);

			if (executeResponse.getType() == ResponseType.SUCCESS) {
				ExecutionResults results = executeResponse.getResult();

				Compra c = (Compra) results.getValue("Compra");
				System.out.println(c);
			} else {

				String message = "Error calculating prices. " + executeResponse.getMsg();
				throw new KieServicesException(message);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}

	}

	@Test
//	@Ignore
	/**
	 * 	Um cliente de nivel 1 quando comprar acima de 10 itens deve ter 5% de desconto
	 */
	public void validarDescontoParaClienteNivel1() {

		KieServicesConfiguration conf = KieServicesFactory
				.newRestConfiguration("http://localhost:8080/kie-server/services/rest/server", "rhdmAdmin", "r3dh4t1!");
		conf.setMarshallingFormat(MarshallingFormat.JSON);
		KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);

		RuleServicesClient rulesClient = kieServicesClient.getServicesClient(RuleServicesClient.class);

		List<ProdutoCompra> prods = new ArrayList<>();
		prods.add(ProdutoCompra.builder()
				.produto(Produto.builder().codigo("123").descricao("abc").valorUnitario(10.0).build()).quantidade(8).valorTotal(80.0)
				.build());
		prods.add(ProdutoCompra.builder()
				.produto(Produto.builder().codigo("321").descricao("xyz").valorUnitario(10.0).build()).quantidade(5).valorTotal(50.0)
				.build());

		Compra compra = Compra.builder().cliente(Cliente.builder().nivel(1).build()).totalDescontos(0.0)
				.valorTotal(0.00).produtos(prods).build();

		KieCommands commandsFactory = KieServices.Factory.get().getCommands();
		List<Command<?>> commands = new ArrayList<>();

		commands.add(commandsFactory.newInsert(compra, "Compra", true, "DEFAULT"));
		commands.add(commandsFactory.newStartProcess("totalizar"));
		commands.add(commandsFactory.newFireAllRules());

		BatchExecutionCommand batchCommand = commandsFactory.newBatchExecution(commands, KIE_SESSION_NAME);
		ServiceResponse<ExecutionResults> executeResponse = rulesClient.executeCommandsWithResults(CONTAINER_ID,
				batchCommand);

		if (executeResponse.getType() == ResponseType.SUCCESS) {
			ExecutionResults results = executeResponse.getResult();

			Compra c = (Compra) results.getValue("Compra");

			assertThat(c.getTotalDescontos(), is(0.05));
			assertThat(c.getValorTotal(), is(Matchers.greaterThan(0.00)));

		} else {

			String message = "Error calculating prices. " + executeResponse.getMsg();
			throw new RuntimeException(message);
		}

	}
	
	@Test
	/**
	 * 	Um cliente de nivel 5 quando comprar qualquer item não paga frete e recebe 25% de desconto
	 */
	public void validarDescontoParaClienteNivel5() {

		KieServicesConfiguration conf = KieServicesFactory
				.newRestConfiguration("http://localhost:8080/kie-server/services/rest/server", "rhdmAdmin", "r3dh4t1!");
		conf.setMarshallingFormat(MarshallingFormat.JSON);
		KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);

		RuleServicesClient rulesClient = kieServicesClient.getServicesClient(RuleServicesClient.class);

		List<ProdutoCompra> prods = new ArrayList<>();
		prods.add(ProdutoCompra.builder()
				.produto(Produto.builder().codigo("123").descricao("abc").valorUnitario(100.0).build()).quantidade(1).valorTotal(100.0)
				.build());		

		Compra compra = Compra.builder().cliente(Cliente.builder().nivel(5).build()).totalDescontos(0.0)
				.valorTotal(0.00).produtos(prods).build();

		KieCommands commandsFactory = KieServices.Factory.get().getCommands();
		List<Command<?>> commands = new ArrayList<>();

		commands.add(commandsFactory.newInsert(compra, "Compra", true, "DEFAULT"));
		commands.add(commandsFactory.newStartProcess("totalizar"));
		commands.add(commandsFactory.newFireAllRules());

		BatchExecutionCommand batchCommand = commandsFactory.newBatchExecution(commands, KIE_SESSION_NAME);
		ServiceResponse<ExecutionResults> executeResponse = rulesClient.executeCommandsWithResults(CONTAINER_ID,
				batchCommand);

		if (executeResponse.getType() == ResponseType.SUCCESS) {
			ExecutionResults results = executeResponse.getResult();

			Compra c = (Compra) results.getValue("Compra");

			assertThat(c.getTotalDescontos(), is(0.25));
			assertThat(c.getFrete(), is(0.0));
			assertThat(c.getValorTotal(), is(75.0));

		} else {

			String message = "Error calculating prices. " + executeResponse.getMsg();
			throw new RuntimeException(message);
		}

	}

	private Compra extractResult(String post) {

		Gson gson = new Gson();
		JsonObject returnObjetct = gson.fromJson(post, JsonObject.class);

		JsonObject obj = returnObjetct.getAsJsonObject("result").getAsJsonObject("execution-results")
				.getAsJsonArray("results").get(0).getAsJsonObject();
		Compra compra = gson.fromJson(obj.getAsJsonObject("value").getAsJsonObject("com.redhat.loja_online.Compra"),
				Compra.class);
		return compra;
	}

	private String mountJson(Compra obj, String processId) {
		Gson gson = new Gson();
		String json = gson.toJson(obj);

		StringBuilder sb = new StringBuilder("{");
		sb.append(
				"\"lookup\":\"kieless\",\"commands\":[{\"insert\":{\"entry-point\":\"DEFAULT\",\"out-identifier\":\"Compra\",\"return-object\":\"true\",");
		sb.append("\"object\":{\"com.redhat.loja_online.Compra\":");
		sb.append(json);
		sb.append("}}},{\"start-process\":{\"processId\":\"" + processId + "\"}},{\"fire-all-rules\":{}}]}");

		return sb.toString();
	}

	/**
	 
	 rule "zerar frete quando nao houver itens"
dialect "java"
ruleflow-group "calcular_total_compra"
when
    $compra : Compra( $produtos : produtos );
    eval($produtos.size() > 0)
then
	$compra.setFrete(0.0);
    $compra.setValorTotal(0.0);
    $compra.setTotalDescontos(0.0);
    update($compra);
end
	 
	 **/
	 
	
}
