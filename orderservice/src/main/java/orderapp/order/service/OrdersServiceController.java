package orderapp.order.service;


import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import orderapp.model.Account;
import orderapp.model.Order;

@RestController
@ConfigurationProperties
public class OrdersServiceController {

	private RestTemplate restTemplate = new RestTemplate();
	
	@Autowired
	OrderService orderService;

	@Value("${accountServiceUrl}")
	private String accountServiceUrl;

	public void setServiceUrl(String accountServiceUrl) {
		this.accountServiceUrl = accountServiceUrl;
	}

	@RequestMapping(value = "/orders", method = RequestMethod.POST)
    public ResponseEntity<Void> createOrder(@RequestBody Order order,
    		@RequestParam(value="createaccount", required=false) String createAccount,
    		@RequestParam(value="name", required=false) String accountName,
    		@RequestParam(value="type", required=false) String accountType, 
    		UriComponentsBuilder ucBuilder) {

		System.out.println("Creating Order: " + order);

        HttpHeaders headers = new HttpHeaders();
        if (createAccount != null && createAccount.equals("true")) {
        	int accountId = createAccount(accountName, accountType);
        	order.setAccountId(accountId);
        } else {
        	if (!isExistingAccount(order.getAccountId())) {
        		return new ResponseEntity<Void>(headers, HttpStatus.NOT_FOUND);
        	}
        }
        int id = orderService.createOrder(order);
        headers.setLocation(ucBuilder.path("/orders/{id}").buildAndExpand(id).toUri());
        return new ResponseEntity<Void>(headers, HttpStatus.CREATED);
	}
	
	@RequestMapping(value = "/orders/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Order> getOrder(@PathVariable("id") String id) {	
		System.out.println("Getting order with id: " + id);
		Order order = orderService.getOrder(Integer.parseInt(id));
		if (order == null) {
			return new ResponseEntity<Order>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Order>(order, HttpStatus.OK);		
	}
	
	protected int createAccount(String name, String type)	{
        if (type == null || type.isEmpty()) {
        	type = "DEFAULT";
        }
        HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(accountServiceUrl).path("/accounts");

		Account account = new Account(name, type);
		HttpEntity<Account> entity = new HttpEntity<Account>(account, headers);
		HttpEntity<String> result = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, entity, String.class);
		//assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
		String location = result.getHeaders().getLocation().toString();
		String id = location.substring(location.lastIndexOf('/')+1);
		return Integer.parseInt(id);
	}
	
	protected boolean isExistingAccount(int id) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(accountServiceUrl).path("/accounts/{id}");

		Map<String, String> prams = new HashMap<String, String>();
		prams.put("id", String.valueOf(id));

		System.out.println("url: " + builder.buildAndExpand(prams).toUri().toString());
		try {
			@SuppressWarnings("unused")
			Account account = restTemplate.getForObject(builder.buildAndExpand(prams).toUri().toString(), Account.class, prams);
		}
		catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND)
				return false;
			throw e;
		}
		return true;
	}
}