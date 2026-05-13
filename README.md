This is a osu!web wrapper used by [Overlay Engine](https://github.com/ZenoXi/osu-overlays).

The following properties need to be defined in `application.properties`:
- `osuweb.client-id`: the client id of a registered osu!web API application
- `osuweb.client-secret`: the client secret of a registered osu!web API application
- `osuweb.endpoint-url`: currently `https://osu.ppy.sh`
- `osuweb.api-path`: currently `/api/v2`

Other properties exist for analytics using PostHog, but they aren't necessary
