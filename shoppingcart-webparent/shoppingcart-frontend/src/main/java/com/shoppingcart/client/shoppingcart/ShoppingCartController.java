package com.shoppingcart.client.shoppingcart;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.shoppingcart.client.ControllerHelper;
import com.shoppingcart.common.entity.CartItem;
import com.shoppingcart.common.entity.Customer;

@Controller
public class ShoppingCartController {
	
	@Autowired private ControllerHelper controllerHelper;
	@Autowired private ShoppingCartService cartService;
	
	@GetMapping("/cart")
	public String viewCart(Model model, HttpServletRequest request) {
		Customer customer = controllerHelper.getAuthenticatedCustomer(request);//kiểm tra customer đã đăng nhập chưa
		List<CartItem> cartItems = cartService.listCartItems(customer);
		
		float estimatedTotal = 0.0F;
		//1 cartItem chứa 1 product và 1 customer
		for (CartItem item : cartItems) {
			estimatedTotal += item.getSubtotal();//tính tổng số tiền của tất cả các cartItem
		}
		
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("estimatedTotal", estimatedTotal);
		
		return "cart/shopping_cart";
	}
}
