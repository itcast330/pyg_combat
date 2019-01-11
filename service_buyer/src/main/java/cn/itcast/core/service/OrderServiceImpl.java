package cn.itcast.core.service;

import cn.itcast.core.dao.log.PayLogDao;
import cn.itcast.core.dao.order.OrderDao;
import cn.itcast.core.dao.order.OrderItemDao;
import cn.itcast.core.pojo.entity.BuyerCart;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.pojo.order.OrderItem;
import cn.itcast.core.pojo.order.OrderItemQuery;
import cn.itcast.core.pojo.order.OrderQuery;
import cn.itcast.core.util.Constants;
import cn.itcast.core.util.IdWorker;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private PayLogDao payLogDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderItemDao orderItemDao;

    @Autowired
    private RedisTemplate redisTemplate;



    @Autowired
    private IdWorker idWorker;


    @Override
    public void add(Order order) {
        //获取当前登录用户的用户名
        String userId = order.getUserId();
        //根据用户名到redis中获取当前用户的购物车集合
        List<BuyerCart> cartList= (List<BuyerCart>)redisTemplate.boundHashOps(Constants.CART_LIST_REDIS).get(userId);

        List<String> orderIdList=new ArrayList();//订单ID列表
        double total_money=0;//总金额 （元）

        if (cartList != null) {
            //1. 遍历购物车集合
            for (BuyerCart cart : cartList) {
                //TODO 2. 根据购物车来形成订单记录
                long orderId = idWorker.nextId();
                System.out.println("sellerId:"+cart.getSellerId());
                Order tborder=new Order();//新创建订单对象
                tborder.setOrderId(orderId);//订单ID
                tborder.setUserId(order.getUserId());//用户名
                tborder.setPaymentType(order.getPaymentType());//支付类型
                tborder.setStatus("1");//状态：未付款
                tborder.setCreateTime(new Date());//订单创建日期
                tborder.setUpdateTime(new Date());//订单更新日期
                tborder.setReceiverAreaName(order.getReceiverAreaName());//地址
                tborder.setReceiverMobile(order.getReceiverMobile());//手机号
                tborder.setReceiver(order.getReceiver());//收货人
                tborder.setSourceType(order.getSourceType());//订单来源
                tborder.setSellerId(cart.getSellerId());//商家ID
                //循环购物车明细
                double money=0;

                //3. 从购物车中获取订单明细集合
                List<OrderItem> orderItemList = cart.getOrderItemList();
                if (orderItemList != null) {
                    //4. 遍历购物明细集合
                    for (OrderItem orderItem : orderItemList) {
                        //TODO 5.根据购物明细对象形成订单详情记录
                        orderItem.setId(idWorker.nextId());
                        orderItem.setOrderId( orderId  );//订单ID
                        orderItem.setSellerId(cart.getSellerId());
                        money+=orderItem.getTotalFee().doubleValue();//金额累加
                        orderItemDao.insertSelective(orderItem);

                    }
                }
                tborder.setPayment(new BigDecimal(money));
                orderDao.insertSelective(tborder);
                orderIdList.add(orderId+"");//添加到订单列表
                total_money+=money;//累加到总金额

            }

            //TODO 6. 计算所有购物车中的总价钱, 形成支付日志记录
            if("1".equals(order.getPaymentType())){//如果是微信支付
                PayLog payLog=new PayLog();
                String outTradeNo=  idWorker.nextId()+"";//支付订单号
                payLog.setOutTradeNo(outTradeNo);//支付订单号
                payLog.setCreateTime(new Date());//创建时间
                //订单号列表，逗号分隔
                String ids=orderIdList.toString().replace("[", "").replace("]", "").replace(" ", "");
                payLog.setOrderList(ids);//订单号列表，逗号分隔
                payLog.setPayType("1");//支付类型
                payLog.setTotalFee( (long)(total_money*100 ) );//总金额(分)
                payLog.setTradeState("0");//支付状态
                payLog.setUserId(order.getUserId());//用户ID
                payLogDao.insertSelective(payLog);//插入到支付日志表

                //TODO 7. 使用用户名作为key, 支付日志对象作为value保存到redis中, 供支付使用
                redisTemplate.boundHashOps("payLog").put(order.getUserId(), payLog);//放入缓存
            }
            //TODO 8. 删除购物车
            redisTemplate.boundHashOps(Constants.CART_LIST_REDIS).delete(order.getUserId());

        }
    }

    @Override
    public PayLog getPayLogByUserName(String userName) {
        PayLog payLog = (PayLog)redisTemplate.boundHashOps("payLog").get(userName);
        return payLog;
    }

    @Override
    public void updatePayStatus(String userName) {
        //1. 根据用户名获取redis中的支付日志对象
        PayLog payLog = (PayLog)redisTemplate.boundHashOps("payLog").get(userName);

        if (payLog != null) {
            //2. 更改支付日志表中的支付状态为已支付
            payLog.setTradeState("1");
            payLogDao.updateByPrimaryKeySelective(payLog);

            //3. 更改订单表中的支付状态
            String orderListStr = payLog.getOrderList();
            String[] split = orderListStr.split(",");
            if (split != null) {
                for (String orderId : split) {
                    Order order = new Order();
                    order.setOrderId(Long.parseLong(orderId));
                    order.setStatus("1");
                    orderDao.updateByPrimaryKeySelective(order);
                }
            }

            //4. 删除redis中的支付日志数据
            redisTemplate.boundHashOps("payLog").delete(userName);
        }
    }

    @Override
    public PageResult findPage(Order order, Integer page, Integer rows) {
        PageHelper.startPage(page, rows );
        OrderQuery orderQuery = new OrderQuery();
        OrderQuery.Criteria criteria = orderQuery.createCriteria();

        if (order != null) {
            if (order.getStatus() != null && !"".equals(order.getStatus())) {
                criteria.andStatusEqualTo(order.getStatus());
            }
            if (order.getSellerId() != null && !"".equals(order.getSellerId())) {
                criteria.andSellerIdEqualTo(order.getSellerId());
            }
        }
        Page<Order> orderList = (Page<Order>)orderDao.selectByExample(orderQuery);
        for (Order order1 : orderList) {
            OrderItemQuery orderItemQuery = new OrderItemQuery();
            OrderItemQuery.Criteria itemQueryCriteria = orderItemQuery.createCriteria();
            itemQueryCriteria.andOrderIdEqualTo(order1.getOrderId());
            List<OrderItem> orderItemList = orderItemDao.selectByExample(orderItemQuery);
            order1.setOrderItemList(orderItemList);
        }
        return new PageResult(orderList.getTotal(), orderList.getResult());

    }
    //修改订单状态 发货4
    @Override
    public void updateOrderStatus(Order order) {
        orderDao.updateByPrimaryKeySelective(order);
    }
}
