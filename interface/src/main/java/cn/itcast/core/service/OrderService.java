package cn.itcast.core.service;

import cn.itcast.core.pojo.entity.OrdersAnalye;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.pojo.order.OrderItem;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface OrderService {

    public void add(Order order);

    public PayLog getPayLogByUserName(String userName);

    public void updatePayStatus(String userName);

    PageResult search(OrderItem orderItem, Integer page, Integer rows);

    List<OrdersAnalye> analye();
}
