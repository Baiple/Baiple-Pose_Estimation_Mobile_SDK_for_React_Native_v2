PRIVATE_NPM_REGISTRY = http://urizip.ogp.kr:8873

all:
	@echo "make login -- login to registry before npm_publish"
	@echo "make publish -- publish package to npm"

login:
	npm set registry $(PRIVATE_NPM_REGISTRY)
	npm adduser --registry=$(PRIVATE_NPM_REGISTRY)

publish:
	npm publish --registry=$(PRIVATE_NPM_REGISTRY)

unpublish:
	npm unpublish --force @baiple/pose-detect

