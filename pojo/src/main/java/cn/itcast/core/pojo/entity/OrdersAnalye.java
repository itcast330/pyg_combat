package cn.itcast.core.pojo.entity;

import cn.itcast.core.pojo.order.Order;

import java.io.Serializable;
import java.util.List;

public class OrdersAnalye implements Serializable {
    private String sellerId;
    private String sellerName;
    //每个卖家订单数
    private Long total;

    public Long getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(Long totalFee) {
        this.totalFee = totalFee;
    }

    private Long totalFee;
    //每个卖家百分比

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getPercent() {
        return percent;
    }

    public void setPercent(Long percent) {
        this.percent = percent;
    }

    private Long percent;

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }



    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

}
