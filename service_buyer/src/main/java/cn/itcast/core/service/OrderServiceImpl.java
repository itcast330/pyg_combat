package cn.itcast.core.service;

import cn.itcast.core.dao.log.PayLogDao;
import cn.itcast.core.dao.order.OrderDao;
import cn.itcast.core.dao.order.OrderItemDao;
import cn.itcast.core.dao.seller.SellerDao;
import cn.itcast.core.pojo.entity.BuyerCart;
import cn.itcast.core.pojo.entity.OrdersAnalye;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.pojo.order.OrderItem;

import cn.itcast.core.pojo.order.OrderItemQuery;
import cn.itcast.core.pojo.order.OrderQuery;
import cn.itcast.core.pojo.seller.Seller;
import cn.itcast.core.util.Constants;
import cn.itcast.core.util.IdWorker;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
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
    @Autowired
    private SellerDao sellerDao;

    public List <OrdersAnalye> analye() {
        List <OrdersAnalye> ordersAnalyeList=new ArrayList <> ( );
        List <Seller> sellerList = sellerDao.selectByExample (null);
        for (Seller seller : sellerList) {
            OrdersAnalye ordersAnalye = new OrdersAnalye ( );
            OrderQuery orderQuery = new OrderQuery ( );
            OrderQuery.Criteria criteria = orderQuery.createCriteria ( );
            criteria.andSellerIdEqualTo (seller.getSellerId ());
            List <Order> orderList = orderDao.selectByExample (orderQuery);
            ordersAnalye.setSellerId (seller.getSellerId ());
            ordersAnalye.setSellerName (seller.getName ());
            ordersAnalye.setTotal ((long) orderList.size ());
            ordersAnalyeList.add (ordersAnalye);
        }
        return ordersAnalyeList;
    }


    @Override
    public PageResult search(OrderItem orderItem, Integer page, Integer rows) {

        String title = orderItem.getTitle ( );
        if (!"".equals (title) && title != null) {
            OrderItemQuery orderItemQuery = new OrderItemQuery ( );
            OrderItemQuery.Criteria criteria = orderItemQuery.createCriteria ( );
            criteria.andTitleLike ("%" + title + "%");
            List <OrderItem> orderItems = orderItemDao.selectByExample (orderItemQuery);


            List <Long> orderIdList = new LinkedList <> ( );
            for (OrderItem item : orderItems) {
                Long orderId = item.getOrderId ( );
                orderIdList.add (orderId);
            }

            PageHelper.startPage (page, rows);
            OrderQuery orderQuery1 = new OrderQuery ( );
            OrderQuery.Criteria criteria1 = orderQuery1.createCriteria ( );
            criteria1.andOrderIdIn (orderIdList);

            Page <Order> orderResult = (Page <Order>) orderDao.selectByExample (orderQuery1);
            for (Order order : orderResult.getResult ( )) {
                OrderItemQuery orderItemQuery2 = new OrderItemQuery ( );
                OrderItemQuery.Criteria criteria2 = orderItemQuery2.createCriteria ( );
                Long orderId = order.getOrderId ( );
                criteria2.andOrderIdEqualTo (orderId);
                List <OrderItem> orderItemList = orderItemDao.selectByExample (orderItemQuery2);
                order.setOrderItemList (orderItemList);
            }

            return new PageResult (orderResult.getTotal ( ), orderResult.getResult ( ));
        }

        PageHelper.startPage (page, rows);
        Page <Order> orderResult = (Page <Order>) orderDao.selectByExample (null);
        for (Order order : orderResult.getResult ( )) {
            OrderItemQuery orderItemQuery = new OrderItemQuery ( );
            OrderItemQuery.Criteria criteria = orderItemQuery.createCriteria ( );
            Long orderId = order.getOrderId ( );
            criteria.andOrderIdEqualTo (orderId);
            List <OrderItem> orderItemList = orderItemDao.selectByExample (orderItemQuery);
            order.setOrderItemList (orderItemList);
        }

        return new PageResult (orderResult.getTotal ( ), orderResult.getResult ( ));
    }


    /* for (Long orderId : orderIdList) {
               Order order = orderDao.selectByPrimaryKey (orderId);
               OrderItemQuery orderItemQuery2 = new OrderItemQuery ( );
               OrderItemQuery.Criteria criteria2 = orderItemQuery.createCriteria ( );
               criteria2.andOrderIdEqualTo (orderId);
               List <OrderItem> orderItemList = orderItemDao.selectByExample(orderItemQuery2);
               order.setOrderItemList (orderItemList);
               orderLikeList.add (order);
           }*/
    @Override
    public void add(Order order) {
        //获取当前登录用户的用户名
        String userId = order.getUserId ( );
        //根据用户名到redis中获取当前用户的购物车集合
        List <BuyerCart> cartList = (List <BuyerCart>) redisTemplate.boundHashOps (Constants.CART_LIST_REDIS).get (userId);

        List <String> orderIdList = new ArrayList ( );//订单ID列表
        double total_money = 0;//总金额 （元）

        if (cartList != null) {
            //1. 遍历购物车集合
            for (BuyerCart cart : cartList) {
                //TODO 2. 根据购物车来形成订单记录
                long orderId = idWorker.nextId ( );
                System.out.println ("sellerId:" + cart.getSellerId ( ));
                Order tborder = new Order ( );//新创建订单对象
                tborder.setOrderId (orderId);//订单ID
                tborder.setUserId (order.getUserId ( ));//用户名
                tborder.setPaymentType (order.getPaymentType ( ));//支付类型
                tborder.setStatus ("1");//状态：未付款
                tborder.setCreateTime (new Date ( ));//订单创建日期
                tborder.setUpdateTime (new Date ( ));//订单更新日期
                tborder.setReceiverAreaName (order.getReceiverAreaName ( ));//地址
                tborder.setReceiverMobile (order.getReceiverMobile ( ));//手机号
                tborder.setReceiver (order.getReceiver ( ));//收货人
                tborder.setSourceType (order.getSourceType ( ));//订单来源
                tborder.setSellerId (cart.getSellerId ( ));//商家ID
                //循环购物车明细
                double money = 0;

                //3. 从购物车中获取订单明细集合
                List <OrderItem> orderItemList = cart.getOrderItemList ( );
                if (orderItemList != null) {
                    //4. 遍历购物明细集合
                    for (OrderItem orderItem : orderItemList) {
                        //TODO 5.根据购物明细对象形成订单详情记录
                        orderItem.setId (idWorker.nextId ( ));
                        orderItem.setOrderId (orderId);//订单ID
                        orderItem.setSellerId (cart.getSellerId ( ));
                        money += orderItem.getTotalFee ( ).doubleValue ( );//金额累加
                        orderItemDao.insertSelective (orderItem);

                    }
                }
                tborder.setPayment (new BigDecimal (money));
                orderDao.insertSelective (tborder);
                orderIdList.add (orderId + "");//添加到订单列表
                total_money += money;//累加到总金额

            }

            //TODO 6. 计算所有购物车中的总价钱, 形成支付日志记录
            if ("1".equals (order.getPaymentType ( ))) {//如果是微信支付
                PayLog payLog = new PayLog ( );
                String outTradeNo = idWorker.nextId ( ) + "";//支付订单号
                payLog.setOutTradeNo (outTradeNo);//支付订单号
                payLog.setCreateTime (new Date ( ));//创建时间
                //订单号列表，逗号分隔
                String ids = orderIdList.toString ( ).replace ("[", "").replace ("]", "").replace (" ", "");
                payLog.setOrderList (ids);//订单号列表，逗号分隔
                payLog.setPayType ("1");//支付类型
                payLog.setTotalFee ((long) (total_money * 100));//总金额(分)
                payLog.setTradeState ("0");//支付状态
                payLog.setUserId (order.getUserId ( ));//用户ID
                payLogDao.insertSelective (payLog);//插入到支付日志表
                redisTemplate.boundHashOps ("payLog").put (order.getUserId ( ), payLog);//放入缓存
            }
            redisTemplate.boundHashOps ("cartList").delete (order.getUserId ( ));

        }
    }

    @Override
    public PayLog getPayLogByUserName(String userName) {
        return null;
    }

    @Override
    public void updatePayStatus(String userName) {

    }

}
