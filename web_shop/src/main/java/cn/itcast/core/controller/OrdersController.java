package cn.itcast.core.controller;


import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.service.OrderService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrdersController {

    @Reference
    private OrderService orderService;

    @RequestMapping("/search")
    public PageResult search(@RequestBody Order order, Integer page, Integer rows) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        order.setSellerId(userName);
        PageResult result = orderService.findPage(order, page, rows);
        return result;
    }

    @RequestMapping("/update")
    public Result update(String shippingName, String shippingCod) {
        System.out.println(shippingName);
        System.out.println(shippingCod);
        return new Result(true, "发货成功");
        /*Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus("3");
        try {
            orderService.updateOrderStatus(order);
            return new Result(true, "发货成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "发货失败");
        }*/
    }
}
