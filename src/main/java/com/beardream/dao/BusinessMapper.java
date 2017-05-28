package com.beardream.dao;

import com.beardream.model.Business;
import com.beardream.model.BusinessDishTag;
import com.beardream.model.UserArticle;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface BusinessMapper {
    int deleteByPrimaryKey(Integer businessId);

    int insert(Business record);

    int insertSelective(Business record);

    Business selectByPrimaryKey(Integer businessId);

    List<Business> findBySelective(Business business);

    int updateByPrimaryKeySelective(Business record);

    int updateByPrimaryKeyWithBLOBs(Business record);

    int updateByPrimaryKey(Business record);

    //商家表 左连接菜品表 和 标签表  查询出商家信息和商家的菜品信息
    List<BusinessDishTag> findBusinessDishTagBySelective (BusinessDishTag businessDishTag);
}