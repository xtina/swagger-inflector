package io.swagger.inflector;

import java.util.Map;

import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class SwaggerInflector extends ResourceConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerInflector.class);
  public SwaggerInflector() {
    String swaggerLocation = System.getProperty("swagger", "swagger.yaml");
    Swagger swagger = new SwaggerParser().read(swaggerLocation);

    if(swagger != null) {
      Map<String, Path> paths = swagger.getPaths();
      Map<String, Model> definitions = swagger.getDefinitions();
      for(String pathString : paths.keySet()) {
        final Resource.Builder builder = Resource.builder();
        Path path = paths.get(pathString);
        builder.path(basePath(swagger.getBasePath(), pathString));
        Operation operation;

        operation = path.getGet();
        if(operation != null) {
          addOperation(pathString, builder, HttpMethod.GET, operation, definitions);
        }
        operation = path.getPost();
        if(operation != null) {
          addOperation(pathString, builder, HttpMethod.POST, operation, definitions);
        }
        operation = path.getPut();
        if(operation != null) {
          addOperation(pathString, builder, HttpMethod.PUT, operation, definitions);
        }
        operation = path.getDelete();
        if(operation != null) {
          addOperation(pathString, builder, HttpMethod.DELETE, operation, definitions);
        }
        operation = path.getOptions();
        if(operation != null) {
          addOperation(pathString, builder, HttpMethod.OPTIONS, operation, definitions);
        }
        operation = path.getPatch();
        if(operation != null) {
          addOperation(pathString, builder, "PATCH", operation, definitions);
        }
      }

      // enable swagger JSON
      enableSwaggerJSON(swagger);

      // enable swagger YAML
      enableSwaggerYAML(swagger);
    }
    else {
      LOGGER.error("No swagger definition detected!  Not much to do...");
    }

    // JSON
    register(JacksonJsonProvider.class);

    // Swagger serializers
    register(SwaggerSerializers.class);
  }

  private String basePath(String basePath, String path) {
    if(StringUtils.isEmpty(basePath)) {
      return path;
    }
    return basePath + path;
  }

  private void enableSwaggerJSON(Swagger swagger) {
    final Resource.Builder builder = Resource.builder();
    builder.path(basePath(swagger.getBasePath(), "/swagger.json"))
      .addMethod(HttpMethod.GET)
      .produces(MediaType.APPLICATION_JSON)
      .handledBy(new SwaggerResourceController(swagger))
      .build();

    registerResources(builder.build());      
  }

  private void enableSwaggerYAML(Swagger swagger) {
    final Resource.Builder builder = Resource.builder();
    builder.path(basePath(swagger.getBasePath(), "/swagger.yaml"))
      .addMethod(HttpMethod.GET)
      .produces("application/yaml")
      .handledBy(new SwaggerResourceController(swagger))
      .build();

    registerResources(builder.build());
  }

  private void addOperation(String pathString, Resource.Builder builder, String method, Operation operation, Map<String, Model> definitions) {
    final ResourceMethod.Builder methodBuilder = builder.addMethod(method);

    // TODO: handle other content types
    methodBuilder.handledBy(new JSONOperationController(pathString, method, operation, definitions))
      .produces(MediaType.APPLICATION_JSON);

    final Resource resource = builder.build();
    registerResources(resource);      
  }
}