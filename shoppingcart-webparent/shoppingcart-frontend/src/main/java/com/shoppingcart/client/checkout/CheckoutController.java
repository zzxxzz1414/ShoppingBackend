package com.shoppingcart.client.checkout;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.shoppingcart.client.ControllerHelper;
import com.shoppingcart.client.Utility;
import com.shoppingcart.common.entity.CartItem;
import com.shoppingcart.common.entity.Customer;
import com.shoppingcart.common.entity.order.Order;
import com.shoppingcart.common.entity.order.PaymentMethod;
import com.shoppingcart.client.order.OrderService;
import com.shoppingcart.client.setting.EmailSettingBag;
import com.shoppingcart.client.setting.SettingService;
import com.shoppingcart.client.shoppingcart.ShoppingCartService;

@Controller
public class CheckoutController {

	@Autowired private CheckoutService checkoutService;
	@Autowired private ControllerHelper controllerHelper;
	@Autowired private ShoppingCartService cartService;
	@Autowired private OrderService orderService;
	@Autowired private SettingService settingService;
	
	@GetMapping("/checkout")
	public String showCheckoutPage(Model model, HttpServletRequest request) {
		Customer customer = controllerHelper.getAuthenticatedCustomer(request);//kiểm tra customer đã đăng nhập chưa
		
		List<CartItem> cartItems = cartService.listCartItems(customer);
		CheckoutInfo checkoutInfo = checkoutService.prepareCheckout(cartItems);
		
		model.addAttribute("customer", customer);
		model.addAttribute("checkoutInfo", checkoutInfo);
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("shippingAddress", customer.toString());
		
		return "checkout/checkout";
	}
	
	@PostMapping("/place_order")
	public String placeOrder(HttpServletRequest request) 
			throws UnsupportedEncodingException, MessagingException {
		String paymentType = request.getParameter("paymentMethod");
		PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentType);
		
		Customer customer = controllerHelper.getAuthenticatedCustomer(request);
		
		List<CartItem> cartItems = cartService.listCartItems(customer);
		CheckoutInfo checkoutInfo = checkoutService.prepareCheckout(cartItems);
		
		Order createdOrder = orderService.createOrder(customer, cartItems, paymentMethod, checkoutInfo);
		cartService.deleteByCustomer(customer);
		sendOrderConfirmationEmail(request, createdOrder);
		
		return "checkout/order_completed";
	}

	private void sendOrderConfirmationEmail(HttpServletRequest request, Order order) 
			throws UnsupportedEncodingException, MessagingException {
		EmailSettingBag emailSettings = settingService.getEmailSettings();
		JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettings);
		mailSender.setDefaultEncoding("utf-8");
		
		String toAddress = order.getCustomer().getEmail();

		String subject = "Confirm of your order ID #[[orderId]]";
		
		String content = "<p>Dear [[name]],</p>"
				+ "<br>"
				+ "<p>This email is to confirm that you have successfully placed an order through our website. Please review the following order summary:</p>"
				+ "<br>"
				+ "<p>- Order ID: [[orderId]]</p>"
				+ "<p>- Order time: [[orderTime]]</p>"
				+ "<p>- Ship to: [[shippingAddress]]</p>"
				+ "<p>- Total: $ [[total]]</p>"
				+ "<p>- Payment method: [[paymentMethod]]</p>"
				+ "<br>"
				+ "<p>We're currently processing your order. Click here to view full details of your order on our website(login required).</p>"
				+ "<br>"
				+ "<p>Thanks for purchasing products at Shopping</p>"
				+ "<p>The Shopping Team.</p>";
		
		subject = subject.replace("[[orderId]]", String.valueOf(order.getId()));
		
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		
		helper.setFrom(emailSettings.getFromAddress(), emailSettings.getSenderName());
		helper.setTo(toAddress);
		helper.setSubject(subject);
		
		DateFormat dateFormatter =  new SimpleDateFormat("HH:mm:ss E, dd MMM yyyy");
		String orderTime = dateFormatter.format(order.getOrderTime());
		
		String totalAmount = String.valueOf(order.getTotal());
		
		content = content.replace("[[name]]", order.getCustomer().getFullName());
		content = content.replace("[[orderId]]", String.valueOf(order.getId()));
		content = content.replace("[[orderTime]]", orderTime);
		content = content.replace("[[shippingAddress]]", order.getShippingAddress());
		content = content.replace("[[total]]", totalAmount);
		content = content.replace("[[paymentMethod]]", order.getPaymentMethod().toString());
		
		helper.setText(content, true);
		mailSender.send(message);		
	}
	
}
