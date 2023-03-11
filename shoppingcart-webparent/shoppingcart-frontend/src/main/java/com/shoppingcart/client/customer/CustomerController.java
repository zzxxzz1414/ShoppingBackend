package com.shoppingcart.client.customer;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shoppingcart.client.Utility;
import com.shoppingcart.common.entity.Country;
import com.shoppingcart.common.entity.Customer;
import com.shoppingcart.client.security.CustomerUserDetails;
import com.shoppingcart.client.setting.EmailSettingBag;
import com.shoppingcart.client.setting.SettingService;

@Controller
public class CustomerController {
	
	@Autowired private CustomerService customerService;
	@Autowired private SettingService settingService;
	
	@GetMapping("/register")
	public String showRegisterForm(Model model) {
		List<Country> listCountries = customerService.listAllCountries();//lấy tất cả Countries để đổ lên dropdown
		
		model.addAttribute("listCountries", listCountries);
		model.addAttribute("pageTitle", "Customer Registration");
		model.addAttribute("customer", new Customer());
		
		return "register/register_form";
	}
	
	@PostMapping("/create_customer")
	public String createCustomer(Customer customer, Model model,
			HttpServletRequest request) throws UnsupportedEncodingException, MessagingException {
		customerService.registerCustomer(customer);
		sendVerificationEmail(request, customer);//gửi mail xác thực
		
		model.addAttribute("pageTitle", "Registration Succeeded!");
		
		return "register/register_success";//trả về trang đăng ký thành công
	}

	private void sendVerificationEmail(HttpServletRequest request, Customer customer) 
			throws UnsupportedEncodingException, MessagingException {
		EmailSettingBag emailSettings = settingService.getEmailSettings();
		JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettings);
		
		String toAddress = customer.getEmail();
		String subject = "Please verify your registration to continue shopping";
		
		String content = "<p>Dear [[name]],</p>"
				+ "<br>"
				+ "<p>Click the link below to verify your account:</p>"
				+ "<br>"
				+ "<p><a href=\"" + "[[URL]]" + "\">VERIFY</a></p>"
				+ "<br>"
				+ "<p>Thanks,</p>"
				+ "<p>The Shopping Team.</p>";
		
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		
		helper.setFrom(emailSettings.getFromAddress(), emailSettings.getSenderName());//nhbtuyen2702@gmail.com, Shopping Team
		helper.setTo(toAddress);//email của customer
		helper.setSubject(subject);//tiêu đề email
		
		content = content.replace("[[name]]", customer.getFullName());//thay [[name]] trong content bằng fullname của customer
		
		String verifyURL = Utility.getSiteURL(request) + "/verify?code=" + customer.getVerificationCode();//http://localhost:8083/ShoppingCartClient/verify?code=fwOtOrKtbx2u7yI7ExYyVLPfaBXXv3vudzfqaF909CT1KEL8I5MVB2iyxX4n4i0l
		
		content = content.replace("[[URL]]", verifyURL);//thay [[URL]] trong content bằng verifyURL
		
		helper.setText(content, true);//nội dung email
		
		mailSender.send(message);//gửi mail cho customer
		
		System.out.println("to Address: " + toAddress);
		System.out.println("Verify URL: " + verifyURL);
	}	
	
	//khi customer bấm vào link verify thì sẽ gửi request đe
	@GetMapping("/verify")
	public String verifyAccount(String code, Model model) {
		boolean verified = customerService.verify(code);
		
		return "register/" + (verified ? "verify_success" : "verify_fail");
	}

	@GetMapping("/account_details")
	public String viewAccountDetails(Model model, HttpServletRequest request) {
		String email = Utility.getEmailOfAuthenticatedCustomer(request);//chỉ những customer đã đăng nhập mới được gọi đến request này
		Customer customer = customerService.getCustomerByEmail(email);
		List<Country> listCountries = customerService.listAllCountries();
		
		model.addAttribute("customer", customer);
		model.addAttribute("listCountries", listCountries);
		
		return "customer/account_form";
	}
	
	@PostMapping("/update_account_details")
	public String updateAccountDetails(Model model, Customer customer, RedirectAttributes ra,
			HttpServletRequest request) {
		customerService.update(customer);
		ra.addFlashAttribute("message", "Your account details have been updated.");
		
		updateNameForAuthenticatedCustomer(customer, request);
		
		String redirectURL = "redirect:/account_details";
		
		return redirectURL;
	}

	private void updateNameForAuthenticatedCustomer(Customer customer, HttpServletRequest request) {
		Object principal = request.getUserPrincipal();
		
		if (principal instanceof UsernamePasswordAuthenticationToken 
				|| principal instanceof RememberMeAuthenticationToken) {
			CustomerUserDetails userDetails = getCustomerUserDetailsObject(principal);//lấy ra customer đang login để thay đổi firstName và lastName -->fullName của customer hiển thị trên html cũng sẽ thay đổi theo
			Customer authenticatedCustomer = userDetails.getCustomer();
			authenticatedCustomer.setFirstName(customer.getFirstName());
			authenticatedCustomer.setLastName(customer.getLastName());
			
		}
	}
	
	private CustomerUserDetails getCustomerUserDetailsObject(Object principal) {
		CustomerUserDetails userDetails = null;
		if (principal instanceof UsernamePasswordAuthenticationToken) {
			UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
			userDetails = (CustomerUserDetails) token.getPrincipal();
		} else if (principal instanceof RememberMeAuthenticationToken) {
			RememberMeAuthenticationToken token = (RememberMeAuthenticationToken) principal;
			userDetails = (CustomerUserDetails) token.getPrincipal();
		}
		
		return userDetails;
	}
}