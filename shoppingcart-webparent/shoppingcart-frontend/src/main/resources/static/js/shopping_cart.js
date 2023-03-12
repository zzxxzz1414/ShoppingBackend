$(document).ready(function() {
	$(".linkMinus").on("click", function(evt) {
		evt.preventDefault();
		decreaseQuantity($(this));
	});
	
	$(".linkPlus").on("click", function(evt) {
		evt.preventDefault();
		increaseQuantity($(this));
	});
	
	$(".linkRemove").on("click", function(evt) {
		evt.preventDefault();
		removeProduct($(this));
	});		
});

function decreaseQuantity(link) {//link là đối tượng JQuery
	productId = link.attr("pid");
	quantityInput = $("#quantity" + productId);
	newQuantity = parseInt(quantityInput.val()) - 1;//lấy quantity hiện tại - 1
	
	if (newQuantity > 0) {
		quantityInput.val(newQuantity);
		updateQuantity(productId, newQuantity);//cập nhật lại quantity dưới db
	} else {
		showWarningModal('Minimum quantity is 1');
	}	
}

function increaseQuantity(link) {//link là đối tượng JQuery
	productId = link.attr("pid");
	quantityInput = $("#quantity" + productId);
	newQuantity = parseInt(quantityInput.val()) + 1;//lấy quantity hiện tại + 1
	
	if (newQuantity <= 5) {
		quantityInput.val(newQuantity);
		updateQuantity(productId, newQuantity);//cập nhật lại quantity dưới db
	} else {
		showWarningModal('Maximum quantity is 5');
	}	
}

function updateQuantity(productId, quantity) {
	url = contextPath + "cart/update/" + productId + "/" + quantity;
	
	$.ajax({
		type: "POST",
		url: url,
		beforeSend: function(xhr) {
			xhr.setRequestHeader(csrfHeaderName, csrfValue);
		}
	}).done(function(updatedSubtotal) {
		updateSubtotal(updatedSubtotal, productId);
		updateTotal();
	}).fail(function() {
		showErrorModal("Error while updating product quantity.");
	});	
}

function updateSubtotal(updatedSubtotal, productId) {
	$("#subtotal" + productId).text(updatedSubtotal);
}

function updateTotal() {
	total = 0.0;
	productCount = 0;
	
	$(".subtotal").each(function(index, element) {//lấy ra tất cả thẻ có class là subtotal -->cộng tất cả giá trị của các thẻ này sẽ ra được total
		productCount++;
		total += parseFloat(element.innerHTML);
	});
	
	if (productCount < 1) {//nếu productCount < 1 -->tất cả product đã bị xóa khỏi cart, hiển thị
		showEmptyShoppingCart();
	} else {
		$("#total").text(total);		
	}
	
}

function showEmptyShoppingCart() {
	$("#sectionTotal").hide();//ẩn nội dung thẻ bên phải
	$("#sectionEmptyCartMessage").removeClass("d-none");//hiển thị lại "You have not chosen any products yet."
}

function removeProduct(link) {
	url = link.attr("href");

	$.ajax({
		type: "DELETE",
		url: url,
		beforeSend: function(xhr) {
			xhr.setRequestHeader(csrfHeaderName, csrfValue);
		}
	}).done(function(response) {
		rowNumber = link.attr("rowNumber");
		removeProductHTML(rowNumber);//remove product trong cart trên html
		updateTotal();
		updateCountNumbers();//khi xóa 1 product ra khỏi cart thì phải cập nhật lại toàn bộ index để lần sau có thể tiếp tục remove product bằng index
		
		showModalDialog("Shopping Cart", response);//hiện modal thông báo đã xóa product khỏi cart
		
	}).fail(function() {
		showErrorModal("Error while removing product.");
	});				
}

function removeProductHTML(rowNumber) {
	$("#row" + rowNumber).remove();
	$("#blankLine" + rowNumber).remove();
}

function updateCountNumbers() {
	$(".divCount").each(function(index, element) {
		element.innerHTML = "" + (index + 1);
	}); 
}
