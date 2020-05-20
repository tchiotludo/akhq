import React, { Component } from 'react';
import Form from '../../../../components/Form/Form';
import Joi from 'joi-browser';
import { get, post } from '../../../../utils/api';
import { uriLatestSchemaVersion, uriUpdateSchema } from '../../../../utils/endpoints';

class SchemaUpdate extends Form {
  state = {
    clusterId: '',
    schemaId: '',
    compatibilityOptions: [
      {
        _id: 'NONE',
        name: 'NONE'
      },
      {
        _id: 'BACKWARD',
        name: 'BACKWARD'
      },
      {
        _id: 'BACKWARD_TRANSITIVE',
        name: 'BACKWARD_TRANSITIVE'
      },
      {
        _id: 'FORWARD',
        name: 'FORWARD'
      },
      {
        _id: 'FORWARD_TRANSITIVE',
        name: 'FORWARD_TRANSITIVE'
      },
      {
        _id: 'FULL',
        name: 'FULL'
      },
      {
        _id: 'FULL_TRANSITIVE',
        name: 'FULL_TRANSITIVE'
      }
    ],
    formData: {
      subject: '',
      compatibility: '',
      schema: ''
    },
    errors: {}
  };

  schema = {
    subject: Joi.string()
      .required()
      .label('Subject'),
    compatibility: Joi.string()
      .required()
      .label('Compatibility Level'),
    schema: Joi.string()
      .required()
      .label('Latest Schema')
  };

  componentDidMount() {
    const { clusterId } = this.props.match.params;
    const { schemaId } = this.props || this.props.match.params;
    this.setState({ clusterId, schemaId }, () => this.getLatestSchemaVersion(clusterId, schemaId));
  }

  async getLatestSchemaVersion() {
    const { history } = this.props;
    const { clusterId, schemaId } = this.state;
    let data = {};
    history.replace({
      ...this.props.location,
      loading: true
    });
    try {
      data = await get(uriLatestSchemaVersion(clusterId, schemaId));

      data = data.data;
      if (data) {
        this.handleLatestSchemaVersion(data);
      }
    } catch (err) {
      if (err.response && err.response.status === 404) {
        history.replace('/page-not-found', { errorData: err });
      } else {
        history.replace('/error', { errorData: err });
      }
    } finally {
      history.replace({
        ...this.props.location,
        loading: false
      });
    }
  }

  handleLatestSchemaVersion = latestSchemaVersion => {
    const { formData } = { ...this.state };

    formData.subject = latestSchemaVersion.subject;
    formData.compatibility = latestSchemaVersion.compatibilityLevel;
    formData.schema = JSON.stringify(JSON.parse(latestSchemaVersion.schema), null, 2);

    this.setState(formData);
  };

  doSubmit() {
    const { clusterId, formData } = this.state;
    const { history } = this.props;
    const body = {
      clusterId,
      subject: formData.subject,
      compatibilityLevel: formData.compatibility,
      schema: formData.schema
    };

    history.replace({
      loading: true
    });
    post(uriUpdateSchema(clusterId, formData.subject), body)
      .then(res => {
        this.props.history.push({
          ...this.props.location,
          showSuccessToast: true,
          successToastMessage: `Schema '${formData.subject}' is updated`,
          loading: false
        });
      })
      .catch(err => {
        console.log('err', err);
        this.props.history.replace({
          ...this.props.location,
          showErrorToast: true,
          errorToastTitle: `Failed to update schema ${formData.subject}`,
          errorToastMessage: err.response.data.message,
          loading: false
        });
      });
  }

  render() {
    const { compatibilityOptions } = this.state;

    return (
      <form
        enctype="multipart/form-data"
        class="khq-form khq-form-config"
        onSubmit={e => this.handleSubmit(e)}
      >
        <fieldset>
          {this.renderInput(
            'subject',
            'Subject',
            'Subject',
            undefined,
            undefined,
            false,
            false,
            false,
            { disabled: true }
          )}
          {this.renderSelect(
            'compatibility',
            'Compatibility Level',
            compatibilityOptions,
            ({ currentTarget: input }) => {
              let { formData } = { ...this.state };
              formData.compatibility = input.value;
              this.setState({ formData });
            }
          )}
          {this.renderJSONInput('schema', 'Latest Schema', value => {
            let { formData } = { ...this.state };
            formData.schema = value;
            this.setState({ formData });
          })}

          {this.renderButton('Update', undefined, undefined, 'submit')}
        </fieldset>
      </form>
    );
  }
}

export default SchemaUpdate;
