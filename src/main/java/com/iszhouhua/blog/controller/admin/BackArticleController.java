package com.iszhouhua.blog.controller.admin;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iszhouhua.blog.common.constant.CodeEnum;
import com.iszhouhua.blog.common.exception.BlogException;
import com.iszhouhua.blog.common.util.Result;
import com.iszhouhua.blog.model.Article;
import com.iszhouhua.blog.model.Tag;
import com.iszhouhua.blog.model.enums.ArticleStatusEnum;
import com.iszhouhua.blog.service.ArticleService;
import com.iszhouhua.blog.service.ArticleTagService;
import com.iszhouhua.blog.service.TagService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台文章控制器
 * @author ZhouHua
 * @since 2018-12-20
 */
@RestController
@RequestMapping("admin/article")
public class BackArticleController {

    @Autowired
    private ArticleService articleService;
    @Autowired
    private TagService tagService;
    @Autowired
    private ArticleTagService articleTagService;

    /**
     * 获取文章集合
     * @param page 分页对象
     * @param article 文章对象
     * @return
     */
    @GetMapping("")
    public Result list(Page<Article> page, Article article){
        QueryWrapper<Article> queryWrapper=new QueryWrapper<>(article);
        if(article.getStatus()==null){
            queryWrapper.in("status",ArticleStatusEnum.DRAFT.getValue(),ArticleStatusEnum.PUBLISHED.getValue());
        }
        IPage<Article> articlePage=articleService.page(page,queryWrapper);
        articlePage.getRecords().forEach(post -> post.setTags(tagService.findTagsByArticleId(post.getId())));
        return Result.success("查询成功",articlePage);
    }

    /**
     * 保存文章
     * @param article 文章对象
     * @return
     */
    @PostMapping("")
    public Result save(@RequestBody Article article){
        if(StringUtils.isBlank(article.getTitle())){
            return Result.fail("标题不能为空");
        }
        if(StringUtils.isBlank(article.getContent())){
            return Result.fail("文章内容不能为空不能为空");
        }
        if(StringUtils.isBlank(article.getUrl())){
            article.setUrl(article.getTitle());
        }
        article.setUpdateTime(new Date());
        boolean res;
        try {
            res=articleService.saveOrUpdate(article);
        }catch (DuplicateKeyException e){
            throw new BlogException(CodeEnum.DUPLICATE_KEY.getValue(),"文章链接或标题重复",e);
        }
        if(!res){
            return Result.fail("保存失败");
        }
        if(!CollectionUtils.isEmpty(article.getTags())){
            Long articleId=article.getId()!=null?article.getId():articleService.findArticleByUrl(article.getUrl()).getId();
            List<Long> tagIds=article.getTags().stream().map(Tag::getId).collect(Collectors.toList());
            res=articleTagService.saveBatch(articleId,tagIds);
            if(!res){
                return Result.fail("文章已成功保存，但关联标签保存失败");
            }
        }
        if(article.getId()==null){
            article=articleService.getOne(new QueryWrapper<>(article));
        }
        return Result.success("保存成功",article);
    }

    /**
     * 获得指定的文章数据
     * @param id 文章ID
     * @return
     */
    @PutMapping("")
    public Result info(Long id){
        Article article=articleService.getById(id);
        article.setTags(tagService.findTagsByArticleId(article.getId()));
        return Result.success("查询成功",article);
    }
}