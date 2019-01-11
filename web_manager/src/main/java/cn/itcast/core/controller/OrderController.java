package cn.itcast.core.controller;

import cn.itcast.core.pojo.entity.OrdersAnalye;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.pojo.order.OrderItem;
import cn.itcast.core.service.GoodsService;

import cn.itcast.core.service.OrderService;

import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Reference
    private OrderService orderService;
;


    @RequestMapping("/search")
    public PageResult search(@RequestBody OrderItem orderItem, Integer page, Integer rows) {

        return orderService.search (orderItem, page, rows);
    }
    @RequestMapping("/analye")
    public List<OrdersAnalye> analye() {

        return orderService.analye ();
    }




}












