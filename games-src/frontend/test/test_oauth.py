import pytest
from werkzeug.exceptions import BadRequest

from frontend import create_app
from frontend.oauth import validate_signature

"""
    curl 'https://semantic.cs.put.poznan.pl/games/' \
  -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7' \
  -H 'Accept-Language: en-US,en;q=0.9,pl;q=0.8' \
  -H 'Cache-Control: max-age=0' \
  -H 'Connection: keep-alive' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'Cookie: PHPSESSID=i46n2ff9n03394f78201tpkop6; rl_anonymous_id=RudderEncrypt%3AU2FsdGVkX1%2BqIzCtT0VsaatqXD7VzqojCKZZ2NsZoNYvdajSUgoudqLyFbQ6BTCM4HJT%2FxxXmRoFhBbq32BXuA%3D%3D; rl_group_id=RudderEncrypt%3AU2FsdGVkX1%2FUnyiKtCiE8pKDfb13pts9EGne%2BBMjIZg%3D; rl_group_trait=RudderEncrypt%3AU2FsdGVkX18xTc9%2BWb8rihR02LVy%2BVmKT1gjzh%2FEFuw%3D; rl_user_id=RudderEncrypt%3AU2FsdGVkX19vfdMsuA%2Fax5sbb832m0Nw5xjH7MecRTJIcB8JJQ4XmjSIRLZYvHTY; rl_trait=RudderEncrypt%3AU2FsdGVkX19iDYW5y1gVtCSsIIlfj46chdAXg4%2BxHlA%3D; session=.eJw1j80KwjAQhF-l7FmKhSqYk4IXxYPgNSChXSUlf2Q3l5a-u2lTbzs7386wEyTCCAImCUn3EkQlISQWx_bQSthl5ZTFsr9LmfZNc-rjiEP19Oydxq5QaJU2BRtwBeqwAeecl8XolKuDKbimt3bEMXXs43KWR_wbNmAk7xRr9128jzK0mozKlo6Hj3gLlOzVGx_5pflikUt2ZxQR0gY2-Y0Z5h_1J02m.Z1LavQ.qZmXF5Ci8RY7X02eeXmpod63NQ4' \
  -H 'Origin: https://ekursy.put.poznan.pl' \
  -H 'Referer: https://ekursy.put.poznan.pl/' \
  -H 'Sec-Fetch-Dest: iframe' \
  -H 'Sec-Fetch-Mode: navigate' \
  -H 'Sec-Fetch-Site: same-site' \
  -H 'Upgrade-Insecure-Requests: 1' \
  -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36' \
  -H 'sec-ch-ua: "Not;A=Brand";v="24", "Chromium";v="128"' \
  -H 'sec-ch-ua-mobile: ?0' \
  -H 'sec-ch-ua-platform: "Linux"' \
  --data-raw 'oauth_version=1.0&oauth_nonce=0fbdfb24980817395416df0c78d9e915&oauth_timestamp=1733485577&oauth_consumer_key=put&user_id=6454&lis_person_sourcedid=&roles=Instructor&context_id=17861&context_label=WIiT-Inf-st-I-sem5-SI-lab-24%2F25-Z-1_1&context_title=Sztuczna+inteligencja+-+laboratoria+%5B2024%2F25%5D&resource_link_title=Narz%C4%99dzie+do+zg%C5%82aszania+graczy&resource_link_description=&resource_link_id=17&context_type=CourseSection&lis_course_section_sourcedid=&lis_result_sourcedid=%7B%22data%22%3A%7B%22instanceid%22%3A%2217%22%2C%22userid%22%3A%226454%22%2C%22typeid%22%3A%222%22%2C%22launchid%22%3A1046378709%7D%2C%22hash%22%3A%22aa1f9f2e131f40ef4235902a9b1719a00373232074958cf99c8dd13e26c9ae18%22%7D&lis_outcome_service_url=https%3A%2F%2Fekursy.put.poznan.pl%2Fmod%2Flti%2Fservice.php&lis_person_name_given=J%C4%99drzej&lis_person_name_family=Potoniec&lis_person_name_full=J%C4%99drzej+Potoniec&ext_user_username=jedrzej.potoniec%40put.poznan.pl&lis_person_contact_email_primary=jedrzej.potoniec%40put.poznan.pl&launch_presentation_locale=pl&ext_lms=moodle-2&tool_consumer_info_product_family_code=moodle&tool_consumer_info_version=2021051718&oauth_callback=about%3Ablank&lti_version=LTI-1p0&lti_message_type=basic-lti-launch-request&tool_consumer_instance_guid=ekursy.put.poznan.pl&tool_consumer_instance_name=PUT+LMS+eKursy&tool_consumer_instance_description=eKursy+Politechniki+Pozna%C5%84skiej&launch_presentation_document_target=iframe&launch_presentation_return_url=https%3A%2F%2Fekursy.put.poznan.pl%2Fmod%2Flti%2Freturn.php%3Fcourse%3D17861%26launch_container%3D3%26instanceid%3D17%26sesskey%3DhGUZqTDxsz&oauth_signature_method=HMAC-SHA1&oauth_signature=DwrrAEg7ks4zD76Pz3rDxyS%2FoTI%3D'
"""
ekursy_data = {
    "oauth_version": "1.0",
    "oauth_nonce": "0fbdfb24980817395416df0c78d9e915",
    "oauth_timestamp": "1733485577",
    "oauth_consumer_key": "put",
    "user_id": "6454",
    "lis_person_sourcedid": "",
    "roles": "Instructor",
    "context_id": "17861",
    "context_label": "WIiT-Inf-st-I-sem5-SI-lab-24/25-Z-1_1",
    "context_title": "Sztuczna inteligencja - laboratoria [2024/25]",
    "resource_link_title": "Narzędzie do zgłaszania graczy",
    "resource_link_description": "",
    "resource_link_id": "17",
    "context_type": "CourseSection",
    "lis_course_section_sourcedid": "",
    "lis_result_sourcedid": """{"data":{"instanceid":"17","userid":"6454","typeid":"2","launchid":1046378709},"hash":"aa1f9f2e131f40ef4235902a9b1719a00373232074958cf99c8dd13e26c9ae18"}""",
    "lis_outcome_service_url": "https://ekursy.put.poznan.pl/mod/lti/service.php",
    "lis_person_name_given": "Jędrzej",
    "lis_person_name_family": "Potoniec",
    "lis_person_name_full": "Jędrzej Potoniec",
    "ext_user_username": "jedrzej.potoniec@put.poznan.pl",
    "lis_person_contact_email_primary": "jedrzej.potoniec@put.poznan.pl",
    "launch_presentation_locale": "pl",
    "ext_lms": "moodle-2",
    "tool_consumer_info_product_family_code": "moodle",
    "tool_consumer_info_version": "2021051718",
    "oauth_callback": "about:blank",
    "lti_version": "LTI-1p0",
    "lti_message_type": "basic-lti-launch-request",
    "tool_consumer_instance_guid": "ekursy.put.poznan.pl",
    "tool_consumer_instance_name": "PUT LMS eKursy",
    "tool_consumer_instance_description": "eKursy Politechniki Poznańskiej",
    "launch_presentation_document_target": "iframe",
    "launch_presentation_return_url": "https://ekursy.put.poznan.pl/mod/lti/return.php?course=17861&launch_container=3&instanceid=17&sesskey=hGUZqTDxsz",
    "oauth_signature_method": "HMAC-SHA1",
    "oauth_signature": "DwrrAEg7ks4zD76Pz3rDxyS/oTI="
}


@pytest.fixture
def app():
    return create_app()


def test_ekursy(app):
    """
    This test is a copy of a real request made by eKursy.
    """
    with app.test_request_context(method='POST', path="/", base_url="https://semantic.cs.put.poznan.pl/games/",
                                  data=ekursy_data, headers=[('Content-Type', 'application/x-www-form-urlencoded')]):
        validate_signature({
            'put': 'deed4739-5d40-4966-ac22-8e0079e774c9'
        }, 1733485579)


def test_ekursy_disregard_timestamp(app):
    del app.config['OAUTH_SIGNATURE_LIFETIME']
    with app.test_request_context(method='POST', path="/", base_url="https://semantic.cs.put.poznan.pl/games/",
                                  data=ekursy_data, headers=[('Content-Type', 'application/x-www-form-urlencoded')]):
        validate_signature({
            'put': 'deed4739-5d40-4966-ac22-8e0079e774c9'
        })


def test_ekursy_very_long_lifetime(app):
    app.config['OAUTH_SIGNATURE_LIFETIME'] = 50 * 365 * 24 * 60 * 60
    with app.test_request_context(method='POST', path="/", base_url="https://semantic.cs.put.poznan.pl/games/",
                                  data=ekursy_data, headers=[('Content-Type', 'application/x-www-form-urlencoded')]):
        validate_signature({
            'put': 'deed4739-5d40-4966-ac22-8e0079e774c9'
        })


def test_ekursy_short_lifetime(app):
    app.config['OAUTH_SIGNATURE_LIFETIME'] = 300
    with app.test_request_context(method='POST', path="/", base_url="https://semantic.cs.put.poznan.pl/games/",
                                  data=ekursy_data, headers=[('Content-Type', 'application/x-www-form-urlencoded')]):
        with pytest.raises(BadRequest):
            validate_signature({
                'put': 'deed4739-5d40-4966-ac22-8e0079e774c9'
            })


@pytest.mark.skip(reason="Nonce validation is currently not implemented.")
def test_ekursy_replay(app):
    with app.test_request_context(method='POST', path="/", base_url="https://semantic.cs.put.poznan.pl/games/",
                                  data=ekursy_data, headers=[('Content-Type', 'application/x-www-form-urlencoded')]):
        validate_signature({
            'put': 'deed4739-5d40-4966-ac22-8e0079e774c9'
        }, 1733485579)
    with app.test_request_context(method='POST', path="/", base_url="https://semantic.cs.put.poznan.pl/games/",
                                  data=ekursy_data, headers=[('Content-Type', 'application/x-www-form-urlencoded')]):
        with pytest.raises(BadRequest):
            validate_signature({
                'put': 'deed4739-5d40-4966-ac22-8e0079e774c9'
            }, 1733485580)


def test_empty(app):
    with app.test_request_context(method='POST', path="/"):
        with pytest.raises(BadRequest):
            validate_signature({})
