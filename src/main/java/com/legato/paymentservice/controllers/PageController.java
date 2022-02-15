package com.legato.paymentservice.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legato.paymentservice.beans.CreditCard;
import com.legato.paymentservice.beans.Customer;
import com.legato.paymentservice.beans.PaymentDTO;
import com.legato.paymentservice.beans.ProcessPaymentDTO;
import com.legato.paymentservice.beans.Product;
import com.legato.paymentservice.beans.ProductsCustomers;
import com.legato.paymentservice.client.CreditCardClient;
import com.legato.paymentservice.client.CustomerClient;

@Controller
public class PageController {

	@Autowired
	private CustomerClient customerClient;
	
	@Autowired
	private CreditCardClient creditCardClient;
	
	@PostMapping("/payment")
	public String payment(Model model, PaymentDTO paymentDTO) {
//		ModelAndView mav = new ModelAndView();
//		mav.setViewName("payment");
		model.addAttribute("amount", paymentDTO.getAmount());
		model.addAttribute("customerId", paymentDTO.getCustomerId());
		model.addAttribute("productList", paymentDTO.getProductList());
		return "payment";
	}
	
	
	@PostMapping("/processPayment")
	public String processPayment(Model mav, ProcessPaymentDTO processPaymentDTO) {
		System.out.println(processPaymentDTO.getPaymentType());
//		ModelAndView mav = new ModelAndView();
		if(processPaymentDTO.getPaymentType() == "netbanking") {
			Customer customer = customerClient.getCustomerById(processPaymentDTO.getCustomerId());
			if(customer != null && (customer.getPassword() == processPaymentDTO.getPassword())) {
				CreditCard creditCardInfo = creditCardClient.getCCById(customer.getCustomerId());
				float customerBal = creditCardInfo.getAmount();
				float amount = processPaymentDTO.getAmount();
				if(customerBal > amount) {
					customerBal -= amount;
					creditCardInfo.setAmount(customerBal);
					ResponseEntity<String> entity = creditCardClient.updateCC(creditCardInfo);
					String body = entity.getBody();
					MediaType contentType = entity.getHeaders().getContentType();
					HttpStatus statusCode = entity.getStatusCode();
					if(statusCode.is2xxSuccessful()) {
						List<ProductsCustomers> productCustomersList = new ArrayList<>();
						List<Product> productList = convertStringToList(processPaymentDTO.getProductlist());
						for(Product product : productList) {
							ProductsCustomers productsCustomers = new ProductsCustomers();
							productsCustomers.setCreditCardNumber(creditCardInfo.getCreditCardNumber());
							productsCustomers.setCustomerId(customer.getCustomerId());
							productsCustomers.setProductId(product.getProductId());
							productsCustomers.setPrice(product.getPrice());
							productsCustomers.setDebitCardNumber(customer.getDebitCardNumber());
						}
						// Call the CRUD service to save the data
						mav.addAttribute("message", "Order Placed");
						return "success";
					}
					
				}	
			}
			
		}
		
		/** 
		 * This code will be executed in case the authentication fails.
		 * The User will be again shown the payment page
		 */
		mav.addAttribute("amount", processPaymentDTO.getAmount());
		mav.addAttribute("productList", processPaymentDTO.getProductlist());
		mav.addAttribute("customerId", processPaymentDTO.getCustomerId());
		return "payment";
	}
	
	private List<Product> convertStringToList(List<String> list) {
		List<Product> products = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		for (int i = 0; i < list.size(); i++) {
			try {
	        	String productStr = list.get(i);
	            Product product = mapper.readValue(productStr, Product.class);

	            System.out.println("artist.getId() = " + product.getItemId());
	            System.out.println("artist.getName() = " + product.getProductName());
	            products.add(product);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		return products;
	}
}
