package com.codewithmosh.store.products;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;


@AllArgsConstructor
@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;


    @GetMapping
    public Iterable<ProductDto> getAllProducts(
            @RequestParam(required = false,defaultValue = "",name = "categoryId") Byte id
    ) {
        System.out.println("called");
        if (id == null) {
            return productRepository.findAllWithCategory().stream().map(productMapper::toDto).toList();
        }
        return productRepository.findByCategoryId(id).stream().map(productMapper::toDto).toList();

    }

    @GetMapping("/{id}")
    public ProductDto getProductById(@PathVariable Long id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        return productMapper.toDto(product);
    }

    @PostMapping

    public ResponseEntity<ProductDto> createProduct(
            @RequestBody ProductDto productDto,
            UriComponentsBuilder uriComponentsBuilder) {
var category = categoryRepository.findById(productDto.getCategoryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));




        var entity = productMapper.toEntity(productDto);
        entity.setCategory(category);
        System.out.println(productDto);
        System.out.println(entity);

        var product =productRepository.save(entity);
      var uri =  uriComponentsBuilder.path("/products/{id}").buildAndExpand(product.getId()).toUri();
      return ResponseEntity.created(uri).body(productMapper.toDto(product));

    }

    @PostMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDto request
    ) {
        var product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

       productMapper.update(request,product);
        product.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        productRepository.save(product);
        return ResponseEntity.ok().body(request);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        var product = productRepository.findById(id).orElse(null);
        if (product == null) {return ResponseEntity.badRequest().build();}

        productRepository.delete(product);

        return ResponseEntity.noContent().build();
    }



}
