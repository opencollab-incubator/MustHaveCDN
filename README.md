# MustHaveCDN

MustHaveCDN is a Geyser extension which automatically uploads all path based packs registered in Geyser to a remote CDN. (S3 and R2 currently supported)

## Setup
- Add `MustHaveCDN-VERSION.jar` to your Geyser `extensions` folder
- Restart your server
- Change the `config.yml` for all the details with your S3/R2 bucket
- Run `/geyser reload` in order to additional reload MustHaveCDN
- All packs will now be distributed via your CDN instead of locally

## But why?

Bedrock unfortunately doesn't support mixing local packs with CDN-based packs. This becomes a problem when trying to use built-in features like custom skulls which *only* generate a local pack, this extension solves it by swapping it out for a CDN-based pack by uploading it to an S3 bucket/R2 bucket.