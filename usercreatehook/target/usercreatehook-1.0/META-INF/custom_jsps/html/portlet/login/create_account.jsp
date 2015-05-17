<%--
/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/html/portlet/login/init.jsp" %>


<%!
/**
 * Organizations comparator.
 * 
 * @author Marian Soban
 */
  class OrganizationComparator implements java.util.Comparator<Organization> {
    
      public int compare(Organization o1, Organization o2) {

          String name1 = o1.getName();
          String name2 = o2.getName();

          return name1.compareTo(name2);
      }
  }
%>

<%

String redirect = ParamUtil.getString(request, "redirect");

String openId = ParamUtil.getString(request, "openId");

PasswordPolicy passwordPolicy = PasswordPolicyLocalServiceUtil.getDefaultPasswordPolicy(company.getCompanyId());

Calendar birthday = CalendarFactoryUtil.getCalendar();

birthday.set(Calendar.MONTH, Calendar.JANUARY);
birthday.set(Calendar.DATE, 1);
birthday.set(Calendar.YEAR, 1970);

boolean male = ParamUtil.getBoolean(request, "male", true);

long parentOrgId = ParamUtil.getLong(request, "parentOrganizationId", -1l);
long subOrgId = ParamUtil.getLong(request, "organizationId", -1l);

List<Organization> orgs = OrganizationLocalServiceUtil.getOrganizations(company.getCompanyId(), -1);
Map<Organization, List<Organization>> organizationsMap = new HashMap<Organization, List<Organization>>();
if (orgs != null) {
    // a) iterate root organizations
    for (Organization org : orgs) {
        if (org.isRoot()) {
            organizationsMap.put(org, new ArrayList<Organization>());
        }
    }
    // b) iterate child organizations
    for (Organization org : orgs) {
        if (!org.isRoot()) {
            Organization parentOrganization = org.getParentOrganization();
            if (parentOrganization != null && organizationsMap.get(parentOrganization) != null) {
                List<Organization> childOrganizations = organizationsMap.get(parentOrganization);
                childOrganizations.add(org);
            }
        }
    }
}

%>

<portlet:actionURL var="createAccoutURL">
	<portlet:param name="saveLastPath" value="0" />
	<portlet:param name="struts_action" value="/login/create_account" />
</portlet:actionURL>

<aui:form action="<%= createAccoutURL %>" method="post" name="fm">
	<aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= Constants.ADD %>" />
	<aui:input name="redirect" type="hidden" value="<%= redirect %>" />
	<aui:input name="openId" type="hidden" value="<%= openId %>" />

	<liferay-ui:error exception="<%= AddressCityException.class %>" message="please-enter-a-valid-city" />
	<liferay-ui:error exception="<%= AddressStreetException.class %>" message="please-enter-a-valid-street" />
	<liferay-ui:error exception="<%= AddressZipException.class %>" message="please-enter-a-valid-postal-code" />
	<liferay-ui:error exception="<%= CaptchaMaxChallengesException.class %>" message="maximum-number-of-captcha-attempts-exceeded" />
	<liferay-ui:error exception="<%= CaptchaTextException.class %>" message="text-verification-failed" />
	<liferay-ui:error exception="<%= CompanyMaxUsersException.class %>" message="unable-to-create-user-account-because-the-maximum-number-of-users-has-been-reached" />
	<liferay-ui:error exception="<%= ContactFirstNameException.class %>" message="please-enter-a-valid-first-name" />
	<liferay-ui:error exception="<%= ContactFullNameException.class %>" message="please-enter-a-valid-first-middle-and-last-name" />
	<liferay-ui:error exception="<%= ContactLastNameException.class %>" message="please-enter-a-valid-last-name" />
	<liferay-ui:error exception="<%= DuplicateUserEmailAddressException.class %>" message="the-email-address-you-requested-is-already-taken" />
	<liferay-ui:error exception="<%= DuplicateUserIdException.class %>" message="the-user-id-you-requested-is-already-taken" />
	<liferay-ui:error exception="<%= DuplicateUserScreenNameException.class %>" message="the-screen-name-you-requested-is-already-taken" />
	<liferay-ui:error exception="<%= EmailAddressException.class %>" message="please-enter-a-valid-email-address" />
	
	<liferay-ui:error key="notPermittedEmailDomainError" message="not-permitted-email-domain" />
	
	<liferay-ui:error exception="<%= GroupFriendlyURLException.class %>">

		<%
		GroupFriendlyURLException gfurle = (GroupFriendlyURLException)errorException;
		%>

		<c:if test="<%= gfurle.getType() == GroupFriendlyURLException.DUPLICATE %>">
			<liferay-ui:message key="the-screen-name-you-requested-is-associated-with-an-existing-friendly-url" />
		</c:if>
	</liferay-ui:error>

	<liferay-ui:error exception="<%= NoSuchCountryException.class %>" message="please-select-a-country" />
	<liferay-ui:error exception="<%= NoSuchListTypeException.class %>" message="please-select-a-type" />
	<liferay-ui:error exception="<%= NoSuchRegionException.class %>" message="please-select-a-region" />
	<liferay-ui:error exception="<%= PhoneNumberException.class %>" message="please-enter-a-valid-phone-number" />
	<liferay-ui:error exception="<%= RequiredFieldException.class %>" message="please-fill-out-all-required-fields" />
	<liferay-ui:error exception="<%= ReservedUserEmailAddressException.class %>" message="the-email-address-you-requested-is-reserved" />
	<liferay-ui:error exception="<%= ReservedUserIdException.class %>" message="the-user-id-you-requested-is-reserved" />
	<liferay-ui:error exception="<%= ReservedUserScreenNameException.class %>" message="the-screen-name-you-requested-is-reserved" />
	<liferay-ui:error exception="<%= TermsOfUseException.class %>" message="you-must-agree-to-the-terms-of-use" />
	<liferay-ui:error exception="<%= UserEmailAddressException.class %>" message="please-enter-a-valid-email-address" />
	<liferay-ui:error exception="<%= UserIdException.class %>" message="please-enter-a-valid-user-id" />

	<liferay-ui:error exception="<%= UserPasswordException.class %>">

		<%
		UserPasswordException upe = (UserPasswordException)errorException;
		%>

		<c:if test="<%= upe.getType() == UserPasswordException.PASSWORD_CONTAINS_TRIVIAL_WORDS %>">
			<liferay-ui:message key="that-password-uses-common-words-please-enter-in-a-password-that-is-harder-to-guess-i-e-contains-a-mix-of-numbers-and-letters" />
		</c:if>

		<c:if test="<%= upe.getType() == UserPasswordException.PASSWORD_INVALID %>">
			<liferay-ui:message key="that-password-is-invalid-please-enter-in-a-different-password" />
		</c:if>

		<c:if test="<%= upe.getType() == UserPasswordException.PASSWORD_LENGTH %>">
			<%= LanguageUtil.format(pageContext, "that-password-is-too-short-or-too-long-please-make-sure-your-password-is-between-x-and-512-characters", String.valueOf(passwordPolicy.getMinLength()), false) %>
		</c:if>

		<c:if test="<%= upe.getType() == UserPasswordException.PASSWORD_TOO_TRIVIAL %>">
			<liferay-ui:message key="that-password-is-too-trivial" />
		</c:if>

		<c:if test="<%= upe.getType() == UserPasswordException.PASSWORDS_DO_NOT_MATCH %>">
			<liferay-ui:message key="the-passwords-you-entered-do-not-match-each-other-please-re-enter-your-password" />
		</c:if>
	</liferay-ui:error>

	<liferay-ui:error exception="<%= UserScreenNameException.class %>" message="please-enter-a-valid-screen-name" />
	<liferay-ui:error exception="<%= WebsiteURLException.class %>" message="please-enter-a-valid-url" />

	<c:if test='<%= SessionMessages.contains(request, "missingOpenIdUserInformation") %>'>
		<div class="portlet-msg-info">
			<liferay-ui:message key="you-have-successfully-authenticated-please-provide-the-following-required-information-to-access-the-portal" />
		</div>
	</c:if>

	<aui:model-context model="<%= Contact.class %>" />
	
	<div id="helptext"><liferay-ui:message key="create-account-help" /></div>
	
	<aui:fieldset>
		<aui:column>
	
		
			<script type="text/javascript">
				var data = new Array();
			<%
				int i=0;
				java.util.List<Organization> orgs1 = new ArrayList<Organization>();
				orgs1.addAll(organizationsMap.keySet());
				java.util.Collections.sort(orgs1, new OrganizationComparator());
				for (Organization parentOrganization : orgs1) {
				    String parentOrganizationIdStr = String.valueOf(parentOrganization.getOrganizationId()); 
			%>
					data[<%= i %>] = new Array();
			<%
					List<Organization> subOrganizations = organizationsMap.get(parentOrganization);
					int j=0;
					java.util.Collections.sort(subOrganizations, new OrganizationComparator());
					for (Organization subOrganization : subOrganizations) {
					    subOrganization = subOrganization.toEscapedModel();
					    String subOrganizationIdStr = String.valueOf(subOrganization.getOrganizationId());
			%>
						data[<%= i %>][<%= j %>] = new Array();
						data[<%= i %>][<%= j %>][0] = '<%= subOrganization.getOrganizationId() %>';
						data[<%= i %>][<%= j %>][1] = '<%= subOrganization.getName() %>';
			<%
						j++;
					}
					i++;
				}
			%>
				function parentOrganizationChanged(selectedIndexOfParentOrganization) {
					var organizationSelect = document.<portlet:namespace />fm.<portlet:namespace />organizationId;
					organizationSelect.options.length = 1; // let empty option
					if(selectedIndexOfParentOrganization != "") {
						// fill combo with suborganizations
						i = parseInt(selectedIndexOfParentOrganization);
						i = i - 1;
						if(i >= 0 && data[i].length > 0) {
							for(j=0; j<data[i].length; j++) {
								var opt = new Option();
								opt.value = data[i][j][0];
								opt.text = data[i][j][1];
								organizationSelect.options[organizationSelect.options.length] = opt;
							}
						}
					}
				}
			</script>
			
			<aui:select label="parent-organization" name="parentOrganizationId" onchange="parentOrganizationChanged(this.selectedIndex);">
				<aui:option label=""  value="" />			
			<%
				java.util.List<Organization> orgs2 = new ArrayList<Organization>();
				orgs2.addAll(organizationsMap.keySet());
				java.util.Collections.sort(orgs2, new OrganizationComparator());			
				for (Organization parentOrganization : orgs2) {
				    parentOrganization = parentOrganization.toEscapedModel();
			%>
				<aui:option label="<%= parentOrganization.getName() %>"  value="<%= parentOrganization.getOrganizationId() %>" 
					selected="<%= parentOrganization.getOrganizationId() == parentOrgId %>" />
			<%
			}
			%>
			</aui:select>
			
			<aui:script use="aui-form-validator">
			var validator1 = new A.FormValidator({
				boundingBox: document.<portlet:namespace/>fm,
				
				rules: {
					<portlet:namespace/>parentOrganizationId: {
						required: true
					}
				},
				
				fieldStrings: {
					<portlet:namespace/>parentOrganizationId: {
						required: '<liferay-ui:message key="required" />'
					}
				}
			})
			</aui:script>			
			
			<aui:select label="organization" name="organizationId">
				<aui:option label=""  value="" />
				<%
					if (parentOrgId >= 0l) {
					    Organization parentOrganization = null;
						for (Organization org : organizationsMap.keySet()) {
						    if (org.getOrganizationId() == parentOrgId) {
						        // we found parent organization in map
						        parentOrganization = org;
						        break;
						    }
						}
						if (parentOrganization != null) {
						    List<Organization> subOrganizations = organizationsMap.get(parentOrganization);
							int j=0;
							java.util.Collections.sort(subOrganizations, new OrganizationComparator());
							for (Organization subOrganization : subOrganizations) {
							    subOrganization = subOrganization.toEscapedModel();
					    
				%>
					<aui:option label="<%= subOrganization.getName() %>"  value="<%= subOrganization.getOrganizationId() %>" 
						selected="<%= subOrganization.getOrganizationId() == subOrgId %>" />				
				<%
							}
						}
					}
				%>			
			</aui:select>		
		
			<aui:input model="<%= User.class %>" name="firstName" />
			
<%-- 			<aui:input model="<%= User.class %>" name="middleName" /> --%>

			<aui:input model="<%= User.class %>" name="lastName">
				<c:if test="<%= PrefsPropsUtil.getBoolean(company.getCompanyId(), PropsKeys.USERS_LAST_NAME_REQUIRED, PropsValues.USERS_LAST_NAME_REQUIRED) %>">
					<aui:validator name="required" />
				</c:if>
			</aui:input>

			<c:if test="<%= !PrefsPropsUtil.getBoolean(company.getCompanyId(), PropsKeys.USERS_SCREEN_NAME_ALWAYS_AUTOGENERATE) %>">
				<aui:input model="<%= User.class %>" name="screenName" />
			</c:if>

			<aui:input model="<%= User.class %>" name="emailAddress">
				<c:if test="<%= PrefsPropsUtil.getBoolean(company.getCompanyId(), PropsKeys.USERS_EMAIL_ADDRESS_REQUIRED, PropsValues.USERS_EMAIL_ADDRESS_REQUIRED) %>">
					<aui:validator name="required" />
				</c:if>
			</aui:input>
			
			
			
			<c:if test="<%= PropsValues.LOGIN_CREATE_ACCOUNT_ALLOW_CUSTOM_PASSWORD %>">
				<aui:input label="password" name="password1" size="30" type="password" value="" />

				<aui:input label="enter-again" name="password2" size="30" type="password" value="">
					<aui:validator name="equalTo">
						'#<portlet:namespace />password1'
					</aui:validator>
				</aui:input>
			</c:if>
			<c:choose>
				<c:when test="<%= PrefsPropsUtil.getBoolean(company.getCompanyId(), PropsKeys.FIELD_ENABLE_COM_LIFERAY_PORTAL_MODEL_CONTACT_BIRTHDAY) %>">
					<aui:input name="birthday" value="<%= birthday %>" />
				</c:when>
				<c:otherwise>
					<aui:input name="birthdayMonth" type="hidden" value="<%= Calendar.JANUARY %>" />
					<aui:input name="birthdayDay" type="hidden" value="1" />
					<aui:input name="birthdayYear" type="hidden" value="1970" />
				</c:otherwise>
			</c:choose>
			<c:if test="<%= PrefsPropsUtil.getBoolean(company.getCompanyId(), PropsKeys.FIELD_ENABLE_COM_LIFERAY_PORTAL_MODEL_CONTACT_MALE) %>">
				<aui:select label="gender" name="male">
					<aui:option label="male" value="1" />
					<aui:option label="female" selected="<%= !male %>" value="0" />
				</aui:select>
			</c:if>

			<c:if test="<%= PropsValues.CAPTCHA_CHECK_PORTAL_CREATE_ACCOUNT %>">
				<portlet:actionURL var="captchaURL" windowState="<%= LiferayWindowState.EXCLUSIVE.toString() %>">
					<portlet:param name="struts_action" value="/login/captcha" />
				</portlet:actionURL>
<%-- 				<liferay-ui:captcha url="<%= captchaURL %>" /> --%>

				<aui:layout>
					<aui:column>
						<liferay-ui:captcha url="<%= captchaURL %>" />
					</aui:column>                           
					<aui:column>
						<a href="#" class="captcha-reload">
							<img src="/html/img/refresh.png" alt="Reload-Capcha" />
							<liferay-ui:message key="captcha-reload" />
						</a>
					</aui:column>
				</aui:layout>
				<script src="/html/js/jquery-1.7.1.min.js" type="text/javascript"></script>
				<script type="text/javascript">
					jQuery(".captcha-reload").click(function() {
						jQuery(".captcha").attr("src", jQuery(".captcha").attr("src")+"&force=" + new Date().getMilliseconds());
						return false;
					});
				</script>			
			</c:if>
			
		</aui:column>
	</aui:fieldset>
	<aui:button-row>
		<aui:button type="submit" />
	</aui:button-row>
</aui:form>

<liferay-util:include page="/html/portlet/login/navigation.jsp" />

<c:if test="<%= windowState.equals(WindowState.MAXIMIZED) %>">
	<aui:script>
		<%--Liferay.Util.focusFormField(document.<portlet:namespace />fm.<portlet:namespace />firstName);  --%>
		Liferay.Util.focusFormField(document.<portlet:namespace />fm.<portlet:namespace />parentOrganizationId);
	</aui:script>
</c:if>