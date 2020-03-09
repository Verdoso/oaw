/*******************************************************************************
* Copyright (C) 2012 INTECO, Instituto Nacional de Tecnologías de la Comunicación, 
* This program is licensed and may be used, modified and redistributed under the terms
* of the European Public License (EUPL), either version 1.2 or (at your option) any later 
* version as soon as they are approved by the European Commission.
* Unless required by applicable law or agreed to in writing, software distributed under the 
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
* ANY KIND, either express or implied. See the License for the specific language governing 
* permissions and more details.
* You should have received a copy of the EUPL1.2 license along with this program; if not, 
* you may find it at http://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017D0863
* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
* Modificaciones: MINHAFP (Ministerio de Hacienda y Función Pública) 
* Email: observ.accesibilidad@correo.gob.es
******************************************************************************/
package es.inteco.rastreador2.export.database.utils;

import es.inteco.rastreador2.dao.export.database.Category;
import es.inteco.rastreador2.dao.export.database.Observatory;
import es.inteco.rastreador2.dao.export.database.Page;
import es.inteco.rastreador2.dao.export.database.Site;
import es.inteco.rastreador2.export.database.form.CategoryForm;
import es.inteco.rastreador2.export.database.form.ObservatoryForm;
import es.inteco.rastreador2.export.database.form.PageForm;
import es.inteco.rastreador2.export.database.form.SiteForm;
import org.apache.commons.beanutils.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public final class ExportDataBaseUtils {

    private ExportDataBaseUtils() {
    }

    public static ObservatoryForm getObservatoryForm(Observatory observatory) throws Exception {
        ObservatoryForm observatoryForm = new ObservatoryForm();
        BeanUtils.copyProperties(observatoryForm, observatory);
        List<CategoryForm> categoryFormList = new ArrayList<>();
        if (observatory.getCategoryList() != null) {
            for (Category category : observatory.getCategoryList()) {
                categoryFormList.add(getCategoryForm(category));
            }
        }
        observatoryForm.setCategoryFormList(categoryFormList);
        return observatoryForm;
    }

    public static CategoryForm getCategoryForm(Category category) throws Exception {
        CategoryForm categoryForm = new CategoryForm();

        BeanUtils.copyProperties(categoryForm, category);
        List<SiteForm> siteFormList = new ArrayList<>();
        for (Site site : category.getSiteList()) {
            siteFormList.add(getSiteForm(site));
        }
        categoryForm.setSiteFormList(siteFormList);
        return categoryForm;
    }

    public static SiteForm getSiteForm(Site site) throws Exception {
        SiteForm siteForm = new SiteForm();

        BeanUtils.copyProperties(siteForm, site);
        List<PageForm> pageFormList = new ArrayList<>();
        for (Page page : site.getPageList()) {
            PageForm pageForm = new PageForm();
            BeanUtils.copyProperties(pageForm, page);
            pageFormList.add(pageForm);
        }
        siteForm.setPageList(pageFormList);
        return siteForm;
    }

}