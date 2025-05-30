package cancel.service;

import cancel.entity.*;
import edu.fudan.common.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author fdse
 */
@Service
public class CancelServiceImpl implements CancelService {

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelServiceImpl.class);
    private final String zuul_notification = "http://zuul-notification";
    private final String zuul_order = "http://zuul-order";
    private final String zuul_order_other = "http://zuul-order-other";
    private final String zuul_inside_payment = "http://zuul-inside-payment";
    private final String zuul_user = "http://zuul-user";

    String orderStatusCancelNotPermitted = "Order Status Cancel Not Permitted";

    @Override
    public Response cancelOrder(String orderId, String loginId, HttpHeaders headers) {

        Response<Order> orderResult = getOrderByIdFromOrder(orderId, headers);
        if (orderResult.getStatus() == 1) {
            CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Order found G|H");
            Order order =  orderResult.getData();
            if (order.getStatus() == OrderStatus.NOTPAID.getCode()
                    || order.getStatus() == OrderStatus.PAID.getCode() || order.getStatus() == OrderStatus.CHANGE.getCode()) {

//                order.setStatus(OrderStatus.CANCEL.getCode());

                Response changeOrderResult = cancelFromOrder(order, headers);
                // 0 -- not find order   1 - cancel success
                if (changeOrderResult.getStatus() == 1) {

                    CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Success.");
                    //Draw back money
                    String money = calculateRefund(order);
                    boolean status = drawbackMoney(money, loginId, headers);
                    if (status) {
                        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Draw Back Money] Success.");



                        Response<User> result = getAccount(order.getAccountId().toString(), headers);
                        if (result.getStatus() == 0) {
                            return new Response<>(0, "Cann't find userinfo by user id.", null);
                        }
                        NotifyInfo notifyInfo = new NotifyInfo();
                        notifyInfo.setDate(new Date().toString());
                        notifyInfo.setEmail(result.getData().getEmail());
                        notifyInfo.setStartingPlace(order.getFrom());
                        notifyInfo.setEndPlace(order.getTo());
                        notifyInfo.setUsername(result.getData().getUserName());
                        notifyInfo.setSeatNumber(order.getSeatNumber());
                        notifyInfo.setOrderNumber(order.getId().toString());
                        notifyInfo.setPrice(order.getPrice());
                        notifyInfo.setSeatClass(SeatClass.getNameByCode(order.getSeatClass()));
                        notifyInfo.setStartingTime(order.getTravelTime().toString());

                        sendEmail(notifyInfo, headers);

                    } else {
                        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Draw Back Money] Fail.");
                    }
                    return new Response<>(1, "Success.", null);
                } else {
                    CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Fail.Reason: {}", changeOrderResult.getMsg());
                    return new Response<>(0, changeOrderResult.getMsg(), null);
                }

            } else {
                CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Order Status Not Permitted.");
                return new Response<>(0, orderStatusCancelNotPermitted, null);
            }
        } else {

            Response<Order> orderOtherResult = getOrderByIdFromOrderOther(orderId, headers);
            if (orderOtherResult.getStatus() == 1) {
                CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Order found Z|K|Other");

                Order order =   orderOtherResult.getData();
                if (order.getStatus() == OrderStatus.NOTPAID.getCode()
                        || order.getStatus() == OrderStatus.PAID.getCode() || order.getStatus() == OrderStatus.CHANGE.getCode()) {

                    CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Order status ok");

//                    order.setStatus(OrderStatus.CANCEL.getCode());
                    Response changeOrderResult = cancelFromOtherOrder(order, headers);

                    if (changeOrderResult.getStatus() == 1) {
                        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Success.");
                        //Draw back money
                        String money = calculateRefund(order);
                        boolean status = drawbackMoney(money, loginId, headers);
                        if (status) {
                            CancelServiceImpl.LOGGER.info("[Cancel Order Service][Draw Back Money] Success.");
                        } else {
                            CancelServiceImpl.LOGGER.info("[Cancel Order Service][Draw Back Money] Fail.");
                        }
                        return new Response<>(1, "Success.", null);
                    } else {
                        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Fail.Reason: {}", changeOrderResult.getMsg());
                        return new Response<>(0, "Fail.Reason:" + changeOrderResult.getMsg(), null);
                    }
                } else {
                    CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Order Status Not Permitted.");
                    return new Response<>(0, orderStatusCancelNotPermitted, null);
                }
            } else {
                CancelServiceImpl.LOGGER.info("[Cancel Order Service][Cancel Order] Order Not Found.");
                return new Response<>(0, "Order Not Found.", null);
            }
        }
    }

    public boolean sendEmail(NotifyInfo notifyInfo, HttpHeaders headers) {
        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Send Email]");
        HttpEntity requestEntity = new HttpEntity(notifyInfo, headers);
        ResponseEntity<Boolean> re = restTemplate.exchange(
                zuul_notification + "/api/v1/notifyservice/notification/order_cancel_success",
                HttpMethod.POST,
                requestEntity,
                Boolean.class);
        return re.getBody();
    }

    @Override
    public Response calculateRefund(String orderId, HttpHeaders headers) {

        Response<Order> orderResult = getOrderByIdFromOrder(orderId, headers);
        if (orderResult.getStatus() == 1) {
            Order order =   orderResult.getData();
            if (order.getStatus() == OrderStatus.NOTPAID.getCode()
                    || order.getStatus() == OrderStatus.PAID.getCode()) {
                if (order.getStatus() == OrderStatus.NOTPAID.getCode()) {
                    CancelServiceImpl.LOGGER.info("[Cancel Order][Refund Price] From Order Service.Not Paid.");
                    return new Response<>(1, "Success. Refoud 0", "0");
                } else {
                    CancelServiceImpl.LOGGER.info("[Cancel Order][Refund Price] From Order Service.Paid.");
                    return new Response<>(1, "Success. ", calculateRefund(order));
                }
            } else {
                CancelServiceImpl.LOGGER.info("[Cancel Order][Refund Price] Order. Cancel Not Permitted.");
                return new Response<>(0, "Order Status Cancel Not Permitted, Refound error", null);
            }
        } else {

            Response<Order> orderOtherResult = getOrderByIdFromOrderOther(orderId, headers);
            if (orderOtherResult.getStatus() == 1) {
                Order order =   orderOtherResult.getData();
                if (order.getStatus() == OrderStatus.NOTPAID.getCode()
                        || order.getStatus() == OrderStatus.PAID.getCode()) {
                    if (order.getStatus() == OrderStatus.NOTPAID.getCode()) {
                        CancelServiceImpl.LOGGER.info("[Cancel Order][Refund Price] From Order Other Service.Not Paid.");
                        return new Response<>(1, "Success, Refound 0", "0");
                    } else {
                        CancelServiceImpl.LOGGER.info("[Cancel Order][Refund Price] From Order Other Service.Paid.");
                        return new Response<>(1, "Success", calculateRefund(order));
                    }
                } else {
                    CancelServiceImpl.LOGGER.info("[Cancel Order][Refund Price] Order Other. Cancel Not Permitted.");
                    return new Response<>(0, orderStatusCancelNotPermitted, null);
                }
            } else {
                CancelServiceImpl.LOGGER.info("[Cancel Order][Refund Price] Order not found.");
                return new Response<>(0, "Order Not Found", null);
            }
        }
    }

    private String calculateRefund(Order order) {
        if (order.getStatus() == OrderStatus.NOTPAID.getCode()) {
            return "0.00";
        }
        CancelServiceImpl.LOGGER.info("[Cancel Order] Order Travel Date: {}", order.getTravelDate().toString());
        Date nowDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(order.getTravelDate());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(order.getTravelTime());
        int hour = cal2.get(Calendar.HOUR);
        int minute = cal2.get(Calendar.MINUTE);
        int second = cal2.get(Calendar.SECOND);
        Date startTime = new Date(year,  //NOSONAR
                month,
                day,
                hour,
                minute,
                second);
        CancelServiceImpl.LOGGER.info("[Cancel Order] nowDate  : {}", nowDate);
        CancelServiceImpl.LOGGER.info("[Cancel Order] startTime: {}", startTime);
        if (nowDate.after(startTime)) {
            CancelServiceImpl.LOGGER.info("[Cancel Order] Ticket expire refund 0");
            return "0";
        } else {
            double totalPrice = Double.parseDouble(order.getPrice());
            double price = totalPrice * 0.8;
            DecimalFormat priceFormat = new java.text.DecimalFormat("0.00");
            String str = priceFormat.format(price);
            CancelServiceImpl.LOGGER.info("[Cancel Order]calculate refund - {}", str);
            return str;
        }
    }


    private Response cancelFromOrder(Order order, HttpHeaders headers) {
        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Change Order Status] Changing....");

        HttpEntity requestEntity = new HttpEntity(order, headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                zuul_order + "/api/v1/orderservice/order",
                HttpMethod.PUT,
                requestEntity,
                Response.class);

        return re.getBody();
    }

    private Response cancelFromOtherOrder(Order info, HttpHeaders headers) {
        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Change Order Status] Changing....");
        HttpEntity requestEntity = new HttpEntity(info, headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                zuul_order_other + "/api/v1/orderOtherService/orderOther",
                HttpMethod.PUT,
                requestEntity,
                Response.class);

        return re.getBody();
    }

    public boolean drawbackMoney(String money, String userId, HttpHeaders headers) {
        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Draw Back Money] Draw back money...");

        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                zuul_inside_payment + "/api/v1/inside_pay_service/inside_payment/drawback/" + userId + "/" + money,
                HttpMethod.GET,
                requestEntity,
                Response.class);
        Response result = re.getBody();

        return result.getStatus() == 1;
    }

    public Response<User> getAccount(String orderId, HttpHeaders headers) {
        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Get By Id]");
        HttpEntity requestEntity = new HttpEntity( headers);
        ResponseEntity<Response<User>> re = restTemplate.exchange(
                zuul_user + "/api/v1/userservice/users/id/" + orderId,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Response<User>>() {
                });
        return re.getBody();
    }

    private Response<Order> getOrderByIdFromOrder(String orderId, HttpHeaders headers) {
        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Get Order] Getting....");
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response<Order>> re = restTemplate.exchange(
                zuul_order + "/api/v1/orderservice/order/" + orderId,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Response<Order>>() {
                });
        return re.getBody();
    }

    private Response<Order> getOrderByIdFromOrderOther(String orderId, HttpHeaders headers) {
        CancelServiceImpl.LOGGER.info("[Cancel Order Service][Get Order] Getting....");
        HttpEntity requestEntity = new HttpEntity(  headers);
        ResponseEntity<Response<Order>> re = restTemplate.exchange(
                zuul_order_other + "/api/v1/orderOtherService/orderOther/" + orderId,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Response<Order>>() {
                });
        return re.getBody();
    }

}
