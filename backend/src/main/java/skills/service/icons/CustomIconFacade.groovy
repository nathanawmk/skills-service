package skills.service.icons

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.Validate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.service.auth.UserInfo
import skills.service.auth.UserSkillsGrantedAuthority
import skills.service.datastore.services.AdminProjService
import skills.service.datastore.services.IconService
import skills.storage.model.CustomIcon
import skills.storage.model.ProjDef
import skills.storage.model.auth.RoleName

import javax.transaction.Transactional

/**
 * Created with IntelliJ IDEA.
 * Date: 11/30/18
 * Time: 11:46 AM
 */
@Service
@CompileStatic
@Slf4j
class CustomIconFacade {

    @Autowired
    IconService iconService

    @Autowired
    AdminProjService projectAdminStorageService

    @Autowired
    CssGenerator cssGenerator

    /**
     * Generates a css style sheet containing all the custom icons fore the specified project id
     *
     * @param projectId
     * @return css or an empty string if no custom icons are found
     */
    String generateCss(String projectId){
        Validate.notNull(projectId, "projectId is required")
        Collection<CustomIcon> icons = iconService.getIconsForProject(projectId)
        return cssGenerator.cssify(icons)
    }

    /**
     * Stores an icon and returns the css class to be used for that icon
     * @param projectId
     * @param iconFilename
     * @param contentType
     * @param file
     * @return
     */
    @Transactional
    UploadedIcon saveIcon(String projectId, String iconFilename, String contentType, byte[] file){
        Validate.notNull(iconFilename, "iconFilename is required")
        Validate.notNull(file, "file is required")

        try {
            String dataUri = "data:${contentType};base64,${Base64.getEncoder().encodeToString(file)}"

            ProjDef project = projectAdminStorageService.getProjDef(projectId)

            CustomIcon customIcon = new CustomIcon(projectId: projectId, filename: iconFilename, contentType: contentType, dataUri: dataUri)
            customIcon.setProjDef(project)

            iconService.saveIcon(customIcon)

            String uploadedCss = cssGenerator.cssify([customIcon])
            String cssClassName = IconCssNameUtil.getCssClass(customIcon.projectId, customIcon.filename)

            return new UploadedIcon(cssClassName: cssClassName, cssDefinition: uploadedCss, name: iconFilename)
        }catch(Exception cve){
            log.error("unable to save icon $iconFilename for project $projectId")
            throw cve;
        }
    }

    void deleteIcon(String projectId, String filename){
        Validate.notNull(projectId, "projectId is required")
        Validate.notNull(filename, "filename is required")
        iconService.deleteIcon(projectId, filename)
    }
}
