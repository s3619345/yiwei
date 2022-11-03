/*
*  Copyright 2019-2020 Zheng Jie
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package com.sun.modules.system.service.impl;

import com.sun.modules.system.domain.Dict;
import com.sun.utils.ValidationUtil;
import com.sun.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.sun.modules.system.repository.DictRepository;
import com.sun.modules.system.service.DictService;
import com.sun.modules.system.service.dto.DictDto;
import com.sun.modules.system.service.dto.DictQueryCriteria;
import com.sun.modules.system.service.mapstruct.DictMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.sun.utils.PageUtil;
import com.sun.utils.QueryHelp;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
* @website https://eladmin.vip
* @description 服务实现
* @author admin
* @date 2022-11-03
**/
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final DictRepository dictRepository;
    private final DictMapper dictMapper;

    @Override
    public Map<String,Object> queryAll(DictQueryCriteria criteria, Pageable pageable){
        Page<Dict> page = dictRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root,criteria,criteriaBuilder),pageable);
        return PageUtil.toPage(page.map(dictMapper::toDto));
    }

    @Override
    public List<DictDto> queryAll(DictQueryCriteria criteria){
        return dictMapper.toDto(dictRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root,criteria,criteriaBuilder)));
    }

    @Override
    @Transactional
    public DictDto findById(Long dictId) {
        Dict dict = dictRepository.findById(dictId).orElseGet(Dict::new);
        ValidationUtil.isNull(dict.getDictId(),"Dict","dictId",dictId);
        return dictMapper.toDto(dict);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictDto create(Dict resources) {
        return dictMapper.toDto(dictRepository.save(resources));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Dict resources) {
        Dict dict = dictRepository.findById(resources.getDictId()).orElseGet(Dict::new);
        ValidationUtil.isNull( dict.getDictId(),"Dict","id",resources.getDictId());
        dict.copy(resources);
        dictRepository.save(dict);
    }

    @Override
    public void deleteAll(Long[] ids) {
        for (Long dictId : ids) {
            dictRepository.deleteById(dictId);
        }
    }

    @Override
    public void download(List<DictDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DictDto dict : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("字典名称", dict.getName());
            map.put("描述", dict.getDescription());
            map.put("创建者", dict.getCreateBy());
            map.put("更新者", dict.getUpdateBy());
            map.put("创建日期", dict.getCreateTime());
            map.put("更新时间", dict.getUpdateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}
