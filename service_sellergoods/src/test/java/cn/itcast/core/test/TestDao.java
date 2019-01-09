package cn.itcast.core.test;

import cn.itcast.core.dao.good.BrandDao;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.BrandQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

//加载spring测试环境
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:spring/applicationContext*.xml"})
public class TestDao {

    @Autowired
    private BrandDao brandDao;

    @Test
    public void testFindBrand() {
        //根据主键查询
        Brand brand = brandDao.selectByPrimaryKey(1L);
        System.out.println("=======" + brand);
    }

    @Test
    public void testFindBrandAll() {
        //查询所有数据
        List<Brand> brands = brandDao.selectByExample(null);
        System.out.println("=======" + brands);
    }

    /**
     * 根据查询条件查询品牌表数据
     */
    @Test
    public void testFindBrandByQuery() {
        //创建查询条件对象
        BrandQuery brandQuery = new BrandQuery();
        //设置排序, 根据id降序排序
        brandQuery.setOrderByClause("id desc");
        //不设置默认为不去重
        brandQuery.setDistinct(true);
        //设置查询的列名, 如果不设置默认是查询所有列
        //brandQuery.setFields("id,name");

        //创建where查询对象
        BrandQuery.Criteria criteria = brandQuery.createCriteria();
        //根据名称模糊查询
        criteria.andNameLike("%联%");
        //根据首字母字段模糊查询
        criteria.andFirstCharLike("%L%");
        //根据id精确查询
        criteria.andIdEqualTo(1L);


        List<Brand> brands = brandDao.selectByExample(brandQuery);
        System.out.println("=======" + brands);
    }
}
