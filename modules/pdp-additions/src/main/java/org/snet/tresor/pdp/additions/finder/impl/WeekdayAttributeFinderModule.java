package org.snet.tresor.pdp.additions.finder.impl;

import java.net.URI;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.AttributeFinderModule;

public class WeekdayAttributeFinderModule extends AttributeFinderModule {
	private static final Logger log = LoggerFactory.getLogger(WeekdayAttributeFinderModule.class);

	private static final Set<String> supportedIds;
	static {
		Set<String> set = new HashSet<String>();
		set.add("org:snet:tresor:attribute:weekday");
		supportedIds = Collections.unmodifiableSet(set);
	}

	public WeekdayAttributeFinderModule() { }

	@Override
	public Set<String> getSupportedIds() {
		return supportedIds;
	}

	@Override
	public boolean isDesignatorSupported() {
		return true;
	}

	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer, URI category,
			EvaluationCtx context) {
		log.debug("Retrieving attribute with id {}, type {}, ", attributeId.toString(), attributeType.toString());

		String id = attributeId.toString();
		if (!supportedIds.contains(id))
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));

		String weekdayInt = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
		BagAttribute bag = XACMLHelper.makeBagAttribute(attributeType, weekdayInt);
		return new EvaluationResult(bag);
	}


}
